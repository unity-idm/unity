/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.HazelcastXAResource;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.tx.Propagation;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TxPersistenceException;
import pl.edu.icm.unity.store.rdbms.DBSessionManager;
import pl.edu.icm.unity.store.tx.TransactionsState.TransactionState;

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
	private DBSessionManager db;
	
	@Autowired
	private UserTransactionManager transactionManager;
	
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
				log.warn("Got persistence error from a child transaction, give up", pe);
				throw pe;
			} catch (PersistenceException pe)
			{
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
					transactionManager.rollback();
					throw new TxPersistenceException(pe);
				}

			} finally
			{
				cleanupTransaction(pjp, transactional);
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
				transactionsStack.push(new TransactionState(transactional.propagation(), 
						TransactionTL.getSql(),
						TransactionTL.getHzXAResource()));
			} else if (transactional.propagation() == Propagation.REQUIRE_SEPARATE)
			{
				createNewTransaction(pjp, transactional);
			}
		}
	}
	
	private void createNewTransaction(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		transactionManager.begin();
		Transaction transaction = transactionManager.getTransaction();
		HazelcastXAResource xaResource = hazelcastInstance.getXAResource();
		transaction.enlistResource(xaResource);
		
		if (log.isTraceEnabled())
			log.trace("Starting new transaction for " + pjp.toShortString());
		SqlSession sqlSession = db.getSqlSession(!transactional.noTransaction());
		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		transactionsStack.push(new TransactionState(transactional.propagation(), 
				sqlSession, xaResource));
	}
	
	private void commitIfNeeded(ProceedingJoinPoint pjp, Transactional transactional) 
			throws Exception
	{
		if (!transactional.autoCommit())
			return;

		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		TransactionState ti = transactionsStack.getCurrent();
		
		if (!transactionsStack.isSubtransaction() || ti.getPropagation() == Propagation.REQUIRE_SEPARATE)
		{
			if (log.isTraceEnabled())
				log.trace("Commiting transaction for " + pjp.toShortString());
			transactionManager.getTransaction().delistResource(ti.getHzXAResource(), 
					XAResource.TMSUCCESS);
			transactionManager.commit();
		}
	}

	private void cleanupTransaction(ProceedingJoinPoint pjp, Transactional transactional)
	{
		TransactionsState transactionsStack = TransactionTL.transactionState.get();
		TransactionState ti = transactionsStack.pop();
		
		if (transactionsStack.isEmpty() || ti.getPropagation() == Propagation.REQUIRE_SEPARATE)
		{
			if (log.isTraceEnabled())
				log.trace("Transactions stack is empty for " + pjp.toShortString());
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
