/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.aop;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
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
	
	@Autowired
	private DBSessionManager db;
	
	
	@Around("execution(public * pl.edu.icm.unity.server.api..*.*(..)) && "
			+ "@within(transactional)")
	private Object retryIfNeeded4Class(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return retryIfNeeded(pjp, transactional);
	};
	
	@Around("execution(public * pl.edu.icm.unity.server.api..*.*(..)) && "
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
			if (log.isTraceEnabled())
				log.trace("Starting sql session for " + pjp.toShortString());
			SqlSession sqlSession = db.getSqlSession(true);
			SqlSessionTL.sqlSession.set(sqlSession);
			try
			{
				Object retVal = pjp.proceed();
				if (transactional.autoCommit())
				{
					if (log.isTraceEnabled())
						log.trace("Commiting transaction for " + pjp.toShortString());
					sqlSession.commit();
				}
				return retVal;
			} catch (PersistenceException pe)
			{
				retry++;
				if (retry < transactional.maxRetries())
				{
					log.debug("Got persistence error, will do retry #" + retry + 
							"; " + pjp.toShortString() + 
							"; " + pe.getCause());
					sleepInterruptible(40*retry);
				} else
				{
					log.warn("Got persistence error, give up", pe);
					throw pe;
				}

			} finally
			{
				if (log.isTraceEnabled())
					log.trace("Releassing sql session for " + pjp.toShortString());
				SqlSessionTL.sqlSession.remove();
				db.releaseSqlSession(sqlSession);
			}
		} while(true);
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
