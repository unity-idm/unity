/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.tx;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.rdbms.DBSessionManager;
import pl.edu.icm.unity.store.tx.TransactionEngine;
import pl.edu.icm.unity.store.tx.TransactionalAspect;
import pl.edu.icm.unity.store.tx.TransactionsState;
import pl.edu.icm.unity.store.tx.TxEngineUtils;
import pl.edu.icm.unity.store.tx.TxPersistenceException;

/**
 * RDBMS based transactions layer implementation
 * @author K. Benedyczak
 */
@Component(TransactionEngine.NAME_PFX + "rdbms")
public class SQLTransactionEngine implements TransactionEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, TransactionalAspect.class);
	public static final long RETRY_BASE_DELAY = 50;
	public static final long RETRY_MAX_DELAY = 200;

	private static final Logger testLog = Log.getLogger(Log.BUG_CATCHER, SQLTransactionEngine.class);

	@Autowired
	private DBSessionManager dbSessionMan;
	
	@Override
	public Object runInTransaction(ProceedingJoinPoint pjp, int maxRetries, boolean autoCommit) throws Throwable 
	{
		int retry = 0;
		do
		{
			setupTransactionSession(pjp);
			try
			{
				testLog.trace("Starting transaction in thread {}", Thread.currentThread().getName());
				Object retVal = pjp.proceed();
				commitIfNeeded(pjp, autoCommit);
				testLog.trace("Finished transaction in thread {}", Thread.currentThread().getName());
				return retVal;
			} catch (TxPersistenceException pe)
			{
				log.warn("Got persistence error from a child transaction, give up", pe);
				throw pe;
			} catch (PersistenceException pe)
			{
				retry++;
				if (retry < maxRetries)
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

			} finally
			{
				cleanupTransaction(pjp);
			}
		} while(true);
	}
	
	private void setupTransactionSession(ProceedingJoinPoint pjp)
	{
		TransactionsState<SQLTransactionState> transactionsStack = SQLTransactionTL.getState();
		
		if (transactionsStack.isEmpty())
		{
			if (log.isTraceEnabled())
				log.trace("Starting sql session for " + pjp.toShortString());
			SqlSession sqlSession = dbSessionMan.getSqlSession(true);
			transactionsStack.push(new SQLTransactionState(sqlSession));
		} else
		{
			transactionsStack.push(new SQLTransactionState(SQLTransactionTL.getSql()));
		}
	}
	
	private void commitIfNeeded(ProceedingJoinPoint pjp, boolean autoCommit)
	{
		if (!autoCommit)
			return;

		TransactionsState<SQLTransactionState> transactionsStack = SQLTransactionTL.getState();
		SQLTransactionState ti = transactionsStack.getCurrent();
		
		if (!transactionsStack.isSubtransaction())
		{
			if (log.isTraceEnabled())
				log.trace("Commiting transaction for " + pjp.toShortString());
			
			ti.getSql().commit();
			ti.runPostCommitActions();
		}
	}

	private void cleanupTransaction(ProceedingJoinPoint pjp)
	{
		TransactionsState<SQLTransactionState> transactionsStack = SQLTransactionTL.getState();
		SQLTransactionState ti = transactionsStack.pop();
		
		if (transactionsStack.isEmpty())
		{
			if (log.isTraceEnabled())
				log.trace("Releassing sql session for " + pjp.toShortString());
			ti.getSql().close();
		}
	}
}
