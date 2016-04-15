/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.rdbms.RDBMSDAO;

/**
 * Applies RDBMS mutation described by {@link RDBMSMutationEvent}
 * @author K. Benedyczak
 */
@Component
public class RDBMSMutationEventProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB,
			RDBMSMutationEventProcessor.class);
	
	@Autowired
	private Map<String, RDBMSDAO> daos;
	
	public void apply(RDBMSMutationEvent event, SqlSession sql)
	{
		RDBMSDAO dao = daos.get(event.getDao());
		if (dao == null)
			throw new IllegalStateException("Unknown DAO, this is fatal error: " + event.getDao());
		
		try
		{
			invokeOnDAO(dao, event, sql);
		} catch (Exception e)
		{
			throw new PersistenceException(e);
		}
	}
	
	private void invokeOnDAO(RDBMSDAO dao, RDBMSMutationEvent event, SqlSession sql) 
			throws Exception
	{
		log.trace("Will apply event " + event);
		Object[] args = event.getArgs();
		Class<?>[] argClasses = new Class<?>[args.length];
		for (int i=0; i<argClasses.length; i++)
		{
			//FIXME!
			if (args[i] instanceof String || args[i] instanceof Integer)
				argClasses[i] = args[i].getClass();
			else
				argClasses[i] = Object.class;
		}
		Method method = dao.getClass().getMethod(event.getOperation(), argClasses);

		if (log.isTraceEnabled())
			log.trace("Will use DAO method: " + method);

		method.invoke(dao, event.getArgs());
	}
}
