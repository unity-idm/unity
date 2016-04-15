/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.concurrent.TimeUnit;

import javax.transaction.SystemException;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalQueue;
import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.RDBMSEventSink;
import pl.edu.icm.unity.store.RDBMSEventsBatch;
import pl.edu.icm.unity.store.api.tx.Propagation;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * Aspect providing transaction functionality. SqlSession is set up, released and auto committed 
 * if transactional method requires so (the default). 
 * Failed transactions are retried (again configurable).
 * 
 * The aspect is installed on all public methods of classes implementing interfaces from the 
 * pl.edu.icm.unity.server.api package (and its children). Either the whole class or a method must 
 * be annotated with the {@link Transactional} annotation. The annotation can be used to provide additional settings. 
 * 
 * @author K. Benedyczak
 */
@Component
@Aspect
public class TransactionalAspect
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, TransactionalAspect.class);
	public static final long RETRY_BASE_DELAY = 50;
	public static final long RETRY_MAX_DELAY = 200;
	
	@Autowired
	private HazelcastInstance hazelcastInstance;
	
	
	@Around("execution(public * pl.edu.icm.unity..*.*(..)) && "
			+ "@within(transactional)")
	private Object retryIfNeeded4Class(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return retryIfNeeded(pjp, transactional);
	};
	
	@Around("execution(public * pl.edu.icm.unity..*.*(..)) && "
			+ "@annotation(transactional)")
	public Object retryIfNeeded4Method(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return retryIfNeeded(pjp, transactional);
	}

	private Object retryIfNeeded(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
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
				log.warn("Got persistence error from a child transaction, give up and rollback", pe);
				rollback(pjp);
				throw pe;
			} catch (Exception pe)
			{
				log.debug("Got persistence error, rolling back transaction", pe);				
				rollback(pjp);
				
				TransactionsState transactionsStack = TransactionTL.transactionState.get();
				TransactionState ti = transactionsStack.getCurrent();
				if (transactionsStack.isSubtransaction() && 
						ti.getPropagation() != Propagation.REQUIRE_SEPARATE)
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
					sleepInterruptible(retry);
				} else
				{
					log.warn("Got persistence error, give up", pe);
					throw new TxPersistenceException(pe);
				}

			} finally
			{
				removeTransactionFromStack(pjp, transactional);
			}
		} while(true);
	}
	
	private void setupTransactionSession(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		
		if (transactionsStack.isEmpty())
		{
			createNewTransaction(pjp, transactional);
		} else
		{
			if (transactional.propagation() == Propagation.REQUIRED)
			{
				if (log.isTraceEnabled())
					log.trace("Starting a new not separated subtransaction for " + pjp.toShortString());
				transactionsStack.push(new TransactionState(transactional.propagation(), 
						transactionsStack.getCurrent()));
			} else if (transactional.propagation() == Propagation.REQUIRE_SEPARATE)
			{
				throw new UnsupportedOperationException("Separate subtransactions are not supported");
			}
		}
	}
	
	private void createNewTransaction(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		if (log.isTraceEnabled())
			log.trace("Starting a new transaction for " + pjp.toShortString());
		TransactionContext newTransactionContext = hazelcastInstance.newTransactionContext();
		newTransactionContext.beginTransaction();
		transactionsStack.push(new TransactionState(transactional.propagation(), newTransactionContext));
	}
	
	private void commitIfNeeded(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		TransactionState ti = transactionsStack.getCurrent();
		
		if (!transactionsStack.isSubtransaction() || ti.getPropagation() == Propagation.REQUIRE_SEPARATE)
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
		RDBMSEventsBatch currentRDBMSBatch = TransactionTL.getCurrentRDBMSBatch();
		if (currentRDBMSBatch.getEvents().isEmpty())
			return;
		TransactionContext hzContext = TransactionTL.getHzContext();
		TransactionalQueue<Object> queue = hzContext.getQueue(RDBMSEventSink.RDBMS_EVENTS_QUEUE);
		queue.offer(currentRDBMSBatch, 30, TimeUnit.SECONDS);
	}

	
	private void rollback(ProceedingJoinPoint pjp) throws IllegalStateException, SecurityException, SystemException
	{
		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		TransactionState ti = transactionsStack.getCurrent();
		if (!transactionsStack.isSubtransaction() || ti.getPropagation() == Propagation.REQUIRE_SEPARATE)
		{
			if (log.isTraceEnabled())
				log.trace("Rolling back transaction for " + pjp.toShortString());
			ti.getHzContext().rollbackTransaction();
		}
	}
	
	private void removeTransactionFromStack(ProceedingJoinPoint pjp, Transactional transactional)
	{
		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		transactionsStack.pop();
		
		if (transactionsStack.isEmpty())
		{
			if (log.isTraceEnabled())
				log.trace("Transactions stack is empty for " + pjp.toShortString() + 
						" releasing resources");
		}
	}
	
	
	private void sleepInterruptible(int retryNum)
	{
		long ms = retryNum * RETRY_BASE_DELAY;
		if (ms > RETRY_MAX_DELAY)
			ms = RETRY_MAX_DELAY;
		try
		{
			Thread.sleep(ms);
		} catch (InterruptedException e)
		{
			//ok
		}
	}
}
