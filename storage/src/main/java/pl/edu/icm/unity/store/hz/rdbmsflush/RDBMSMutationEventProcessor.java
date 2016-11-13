/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.rdbmsflush;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.Logger;
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
	
	private Map<String, Map<String, Method>> daoMethods;
	private Map<String, RDBMSDAO> daos;
	
	@Autowired
	public RDBMSMutationEventProcessor(Map<String, RDBMSDAO> daos)
	{
		daoMethods = new HashMap<>();
		for (Map.Entry<String, RDBMSDAO> entry: daos.entrySet())
		{
			Map<String, Method> methods = new HashMap<>();
			daoMethods.put(entry.getKey(), methods);
			
			Class<? extends RDBMSDAO> clazz = entry.getValue().getClass();
			Method[] methodsA = clazz.getMethods();
			for (Method m: methodsA)
			{
				if (m.getDeclaringClass().equals(Object.class) || m.isSynthetic())
					continue;
				
				if (methods.put(m.getName(), m) != null)
					throw new IllegalStateException("RDBMSDAO " + entry.getKey() + 
							" has methods with ambigous names: " + m.getName() + 
							", this is not supported.");
			}
		}
		this.daos = daos;
	}
	
	public void apply(RDBMSMutationEvent event, SqlSession sql)
	{
		Map<String, Method> daoM = daoMethods.get(event.getDao());
		if (daoM == null)
			throw new IllegalStateException("Unknown DAO, this is fatal error: " + event.getDao());
		
		try
		{
			invokeOnDAO(daoM, event, sql);
		} catch (Exception e)
		{
			throw new PersistenceException(e);
		}
	}
	
	private void invokeOnDAO(Map<String, Method> daoM, RDBMSMutationEvent event, SqlSession sql) 
			throws Exception
	{
		Method method = daoM.get(event.getOperation());
		RDBMSDAO dao = daos.get(event.getDao());
		if (log.isTraceEnabled())
			log.trace("Will apply event " + event + " with method " + method);
		method.invoke(dao, event.getArgs());
	}
}
