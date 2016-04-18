/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.tx;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.internal.Transactional;
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
@Component
public class SQLTransactionEngine implements TransactionEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, TransactionalAspect.class);
	public static final long RETRY_BASE_DELAY = 50;
	public static final long RETRY_MAX_DELAY = 200;
	
	@Autowired
	private DBSessionManager dbSessionMan;
	
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
				log.warn("Got persistence error from a child transaction, give up", pe);
				throw pe;
			} catch (PersistenceException pe)
			{
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

			} finally
			{
				cleanupTransaction(pjp, transactional);
			}
		} while(true);
	}
	
	private void setupTransactionSession(ProceedingJoinPoint pjp, Transactional transactional)
	{
		TransactionsState<SQLTransactionState> transactionsStack = SQLTransactionTL.transactionState.get();
		
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
	
	private void commitIfNeeded(ProceedingJoinPoint pjp, Transactional transactional)
	{
		if (!transactional.autoCommit())
			return;

		TransactionsState<SQLTransactionState> transactionsStack = SQLTransactionTL.transactionState.get();
		SQLTransactionState ti = transactionsStack.getCurrent();
		
		if (!transactionsStack.isSubtransaction())
		{
			if (log.isTraceEnabled())
				log.trace("Commiting transaction for " + pjp.toShortString());
			
			ti.getSql().commit();
		}
	}

	private void cleanupTransaction(ProceedingJoinPoint pjp, Transactional transactional)
	{
		TransactionsState<SQLTransactionState> transactionsStack = SQLTransactionTL.transactionState.get();
		SQLTransactionState ti = transactionsStack.pop();
		
		if (transactionsStack.isEmpty())
		{
			if (log.isTraceEnabled())
				log.trace("Releassing sql session for " + pjp.toShortString());
			ti.getSql().close();
		}
	}
}
