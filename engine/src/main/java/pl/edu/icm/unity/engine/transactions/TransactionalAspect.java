/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.transactions;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.transactions.TransactionsState.TransactionState;
import pl.edu.icm.unity.server.utils.Log;

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
	public static final int RETRY_BASE_DELAY = 40;
	
	@Autowired
	private DBSessionManager db;
	
	
	@Around("execution(public * pl.edu.icm.unity.engine..*.*(..)) && "
			+ "@within(transactional)")
	private Object retryIfNeeded4Class(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return retryIfNeeded(pjp, transactional);
	};
	
	@Around("execution(public * pl.edu.icm.unity.engine..*.*(..)) && "
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
			} catch (PersistenceException pe)
			{
				retry++;
				if (retry < transactional.maxRetries())
				{
					log.debug("Got persistence error, will do retry #" + retry + 
							"; " + pjp.toShortString() + 
							"; " + pe.getCause());
					sleepInterruptible(RETRY_BASE_DELAY*retry);
				} else
				{
					log.warn("Got persistence error, give up", pe);
					throw pe;
				}

			} finally
			{
				cleanupTransaction(pjp, transactional);
			}
		} while(true);
	}
	
	private void setupTransactionSession(ProceedingJoinPoint pjp, Transactional transactional)
	{
		TransactionsState transactionsStack = SqlSessionTL.transactionState.get();
		
		if (transactionsStack.isEmpty())
		{
			createNewSqlSession(pjp, transactional);
			transactionsStack.push(new TransactionState(transactional.propagation(), 
					SqlSessionTL.get()));
		} else
		{
			if (transactional.propagation() == Propagation.REQUIRED)
			{
				SqlSession current = SqlSessionTL.get();
				transactionsStack.push(new TransactionState(transactional.propagation(), current));
			} else if (transactional.propagation() == Propagation.REQUIRE_SEPARATE)
			{
				createNewSqlSession(pjp, transactional);
				transactionsStack.push(new TransactionState(transactional.propagation(), 
						SqlSessionTL.get()));
			}
		}
	}
	
	private void createNewSqlSession(ProceedingJoinPoint pjp, Transactional transactional)
	{
		if (log.isTraceEnabled())
			log.trace("Starting sql session for " + pjp.toShortString());
		SqlSession sqlSession = db.getSqlSession(!transactional.noTransaction());
		SqlSessionTL.sqlSession.set(sqlSession);		
	}
	
	private void commitIfNeeded(ProceedingJoinPoint pjp, Transactional transactional)
	{
		if (!transactional.autoCommit())
			return;

		TransactionsState transactionsStack = SqlSessionTL.transactionState.get();
		TransactionState ti = transactionsStack.getCurrent();
		
		if (!transactionsStack.isSubtransaction() || ti.getPropagation() == Propagation.REQUIRE_SEPARATE)
		{
			if (log.isTraceEnabled())
				log.trace("Commiting transaction for " + pjp.toShortString());
			
			ti.getSqlSession().commit();
		}
	}

	private void cleanupTransaction(ProceedingJoinPoint pjp, Transactional transactional)
	{
		TransactionsState transactionsStack = SqlSessionTL.transactionState.get();
		TransactionState ti = transactionsStack.pop();
		
		if (transactionsStack.isEmpty() || ti.getPropagation() == Propagation.REQUIRE_SEPARATE)
		{
			if (log.isTraceEnabled())
				log.trace("Releassing sql session for " + pjp.toShortString());
			SqlSessionTL.sqlSession.remove();
			db.releaseSqlSession(ti.getSqlSession());
		}
	}
	
	
	private void sleepInterruptible(long ms)
	{
		try
		{
			Thread.sleep(ms);
		} catch (InterruptedException e)
		{
			//ok
		}
	}
}
