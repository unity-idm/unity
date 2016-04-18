/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.tx;

import java.util.concurrent.TimeUnit;

import javax.transaction.SystemException;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalQueue;
import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.base.internal.Transactional;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSEventSink;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSEventsBatch;
import pl.edu.icm.unity.store.tx.TransactionEngine;
import pl.edu.icm.unity.store.tx.TransactionalAspect;
import pl.edu.icm.unity.store.tx.TransactionsState;
import pl.edu.icm.unity.store.tx.TxEngineUtils;
import pl.edu.icm.unity.store.tx.TxPersistenceException;

/**
 * Hazelcast based transaction layer implementation.
 * @author K. Benedyczak
 */
@Component
public class HzTransactionEngine implements TransactionEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, TransactionalAspect.class);
	public static final long RETRY_BASE_DELAY = 50;
	public static final long RETRY_MAX_DELAY = 200;
	
	@Autowired
	private HazelcastInstance hazelcastInstance;

	@Override
	public Object runInTransaction(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		int retry = 0;
		do
		{
			setupTransactionSession(pjp, transactional);
			try
			{
				Object retVal = pjp.proceed();
				commitIfNeeded(pjp, transactional);
				return retVal;
			} catch (TxPersistenceException pe)
			{
				TransactionsState<HzTransactionState> transactionsStack = 
						HzTransactionTL.transactionState.get();
				if (transactionsStack.isSubtransaction())
				{
					if (log.isDebugEnabled())
						log.debug("Got error in a subtransaction, propagate to parent; " + 
							pjp.toShortString() + 
							"; " + pe.getCause());
					throw pe;
				} else
				{
					log.warn("Got persistence error from a child transaction, give up and rollback", pe);
					rollback(pjp);
					throw pe;
				}
			} catch (HazelcastException pe) //TODO this should be checked if is correct
			{
				log.debug("Got persistence error, rolling back transaction", pe);				
				rollback(pjp);
				
				TransactionsState<HzTransactionState> transactionsStack = 
						HzTransactionTL.transactionState.get();
				if (transactionsStack.isSubtransaction())
				{
					if (log.isDebugEnabled())
						log.debug("Got persistence error in a subtransaction, propagate to parent; " + 
							pjp.toShortString() + 
							"; " + pe.getCause());
					throw pe;
				}
				
				retry++;
				if (retry < transactional.maxRetries())
				{
					if (log.isDebugEnabled())
						log.debug("Got persistence error, will do retry #" + retry + 
							"; " + pjp.toShortString() + 
							"; " + pe.getCause());
					TxEngineUtils.sleepInterruptible(retry, RETRY_BASE_DELAY, RETRY_MAX_DELAY);
				} else
				{
					log.warn("Got persistence error, give up", pe);
					throw new TxPersistenceException(pe);
				}
			} catch (Throwable t)
			{
				log.debug("Got other error, rolling back transaction and giving up", t);				
				rollback(pjp);
				throw t;
			} finally
			{
				removeTransactionFromStack(pjp, transactional);
			}
		} while(true);
	}
	
	private void setupTransactionSession(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		TransactionsState<HzTransactionState> transactionsStack = HzTransactionTL.transactionState.get();
		
		if (transactionsStack.isEmpty())
		{
			createNewTransaction(pjp, transactional);
		} else
		{
			if (log.isTraceEnabled())
				log.trace("Starting a new not separated subtransaction for " + pjp.toShortString());
			transactionsStack.push(new HzTransactionState(transactionsStack.getCurrent()));
		}
	}
	
	private void createNewTransaction(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		TransactionsState<HzTransactionState> transactionsStack = HzTransactionTL.transactionState.get();
		if (log.isTraceEnabled())
			log.trace("Starting a new transaction for " + pjp.toShortString());
		TransactionContext newTransactionContext = hazelcastInstance.newTransactionContext();
		newTransactionContext.beginTransaction();
		transactionsStack.push(new HzTransactionState(newTransactionContext));
	}
	
	private void commitIfNeeded(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		TransactionsState<HzTransactionState> transactionsStack = HzTransactionTL.transactionState.get();
		HzTransactionState ti = transactionsStack.getCurrent();
		
		if (!transactionsStack.isSubtransaction())
		{
			if (transactional.autoCommit())
			{
				if (log.isTraceEnabled())
					log.trace("Commiting transaction for " + pjp.toShortString());
				
				enqueueRDBMSBatch();
				ti.getHzContext().commitTransaction();
			}
		}
	}

	private void enqueueRDBMSBatch() throws InterruptedException
	{
		RDBMSEventsBatch currentRDBMSBatch = HzTransactionTL.getCurrentRDBMSBatch();
		if (currentRDBMSBatch.getEvents().isEmpty())
			return;
		TransactionContext hzContext = HzTransactionTL.getHzContext();
		TransactionalQueue<Object> queue = hzContext.getQueue(RDBMSEventSink.RDBMS_EVENTS_QUEUE);
		queue.offer(currentRDBMSBatch, 30, TimeUnit.SECONDS);
	}

	
	private void rollback(ProceedingJoinPoint pjp) throws IllegalStateException, SecurityException, SystemException
	{
		TransactionsState<HzTransactionState> transactionsStack = HzTransactionTL.transactionState.get();
		HzTransactionState ti = transactionsStack.getCurrent();
		if (!transactionsStack.isSubtransaction())
		{
			if (log.isTraceEnabled())
				log.trace("Rolling back transaction for " + pjp.toShortString());
			ti.getHzContext().rollbackTransaction();
		}
	}
	
	private void removeTransactionFromStack(ProceedingJoinPoint pjp, Transactional transactional)
	{
		TransactionsState<HzTransactionState> transactionsStack = HzTransactionTL.transactionState.get();
		transactionsStack.pop();
		
		if (transactionsStack.isEmpty())
		{
			if (log.isTraceEnabled())
				log.trace("Transactions stack is empty for " + pjp.toShortString() + 
						" releasing resources");
		}
	}
}
