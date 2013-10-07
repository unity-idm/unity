/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoints;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Implementation of the internal endpoint management. 
 * This is used internally and not exposed by the public interfaces. 
 * @author K. Benedyczak
 */
@Component
public class InternalEndpointManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, InternalEndpointManagement.class);
	private DBSessionManager db;
	private EndpointDB endpointDB;
	private JettyServer httpServer;
	
	@Autowired
	public InternalEndpointManagement(DBSessionManager db, EndpointDB endpointDB,
			JettyServer httpServer)
	{
		this.db = db;
		this.endpointDB = endpointDB;
		this.httpServer = httpServer;
	}

	/**
	 * Queries DB for persisted endpoints and loads them.
	 * Should be run only on startup
	 * @throws EngineException 
	 */
	public synchronized void loadPersistedEndpoints() throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<EndpointInstance> fromDb = endpointDB.getAll(sql);
			for (EndpointInstance instance: fromDb)
			{
				httpServer.deployEndpoint((WebAppEndpointInstance) instance);
				EndpointDescription endpoint = instance.getEndpointDescription();
				log.debug(" - " + endpoint.getId() + ": " + endpoint.getType().getName() + 
						" " + endpoint.getDescription());
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	public synchronized void removeAllPersistedEndpoints() throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			endpointDB.removeAllNoCheck(sql);
			httpServer.undeployAllEndpoints();
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
}
