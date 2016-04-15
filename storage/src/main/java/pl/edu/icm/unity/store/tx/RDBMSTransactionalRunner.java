/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.rdbms.DBSessionManager;

/**
 * Transaction layer wrapper for RDBMS transactions, which are separated from the main 
 * transaction layer used for Hazelcast.
 * Used with lambdas.
 * 
 * @author K. Benedyczak
 */
@Component
public class RDBMSTransactionalRunner implements TransactionalRunner
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, RDBMSTransactionalRunner.class);
	public static final int DEF_MAX_RETRIES = 50;
	public static final long RETRY_BASE_DELAY = 50;
	public static final long RETRY_MAX_DELAY = 200;
	
	@Autowired
	private DBSessionManager dbSessionMan;
	

	@Override
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		throw new UnsupportedOperationException("This method is currently not implemented");
	}
	
	@Override
	public void runInTransaction(TxRunnable code)
	{
		try
		{
			retryIfNeeded(code);
		} catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			else 
				throw new PersistenceException(e);
		}
	}
	
	private void retryIfNeeded(TxRunnable code) throws Exception 
	{
		int retry = 0;
		do
		{
			SqlSession sql = setupTransactionSession();
			try
			{
				code.run();
				commit(sql);
				return;
			} catch (PersistenceException pe)
			{
				log.debug("Got persistence error", pe);				
				retry++;
				if (retry < DEF_MAX_RETRIES)
				{
					if (log.isDebugEnabled())
						log.debug("Got persistence error, will do retry #" + retry + 
							"; " + pe.getCause());
					sleepInterruptible(retry);
				} else
				{
					log.warn("Got persistence error, give up", pe);
					throw pe;
				}
			} finally
			{
				cleanupSession(sql);
			}
		} while(true);
	}
	
	private SqlSession setupTransactionSession() throws Exception
	{
		SqlSession ret = dbSessionMan.getSqlSession(true);
		SQLTransactionTL.setCurrent(ret);
		return ret;
	}
	
	private void commit(SqlSession sql) throws Exception
	{
		sql.commit();
	}

	private void cleanupSession(SqlSession sql)
	{
		SQLTransactionTL.setCurrent(null);
		sql.close();
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
