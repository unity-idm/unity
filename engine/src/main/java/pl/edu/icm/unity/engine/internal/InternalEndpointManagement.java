/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.registries.EndpointFactoriesRegistry;

/**
 * Implementation of the internal endpoint management. 
 * This is used internally and not exposed by the public interfaces. 
 * @author K. Benedyczak
 */
@Component
public class InternalEndpointManagement
{
	public static final String ENDPOINT_OBJECT_TYPE = "endpointDefinition";
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private JettyServer httpServer;
	
	@Autowired
	public InternalEndpointManagement(EndpointFactoriesRegistry endpointFactoriesReg, DBSessionManager db,
			DBGeneric dbGeneric, JettyServer httpServer)
	{
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.db = db;
		this.dbGeneric = dbGeneric;
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
			List<GenericObjectBean> fromDb = dbGeneric.getObjectsOfType(ENDPOINT_OBJECT_TYPE, sql);
			for (GenericObjectBean raw: fromDb)
			{
				EndpointInstance instance = recreateEndpointFromDB(raw.getName(), raw.getSubType(), 
						raw.getContents());
				httpServer.deployEndpoint((WebAppEndpointInstance) instance);
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	public synchronized void removeAllPersistedEndpoints()
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			dbGeneric.removeObjectsByType(ENDPOINT_OBJECT_TYPE, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	
	
	public EndpointInstance recreateEndpointFromDB(String id, String typeId, byte[] serializedState)
	{
		EndpointFactory factory = endpointFactoriesReg.getById(typeId);
		if (factory == null)
			throw new IllegalArgumentException("Endpoint type " + typeId + " is unknown");
		EndpointInstance instance = factory.newInstance();
		instance.setSerializedConfiguration(new String(serializedState, Constants.UTF));
		instance.setId(id);
		return instance;
	}
}
