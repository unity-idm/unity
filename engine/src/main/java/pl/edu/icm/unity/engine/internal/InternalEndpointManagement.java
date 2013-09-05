/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authn.AuthenticatorImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.registries.EndpointFactoriesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
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
	public static final String ENDPOINT_OBJECT_TYPE = "endpointDefinition";
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private JettyServer httpServer;
	private EngineHelper engineHelper;
	
	@Autowired
	public InternalEndpointManagement(EndpointFactoriesRegistry endpointFactoriesReg,
			DBSessionManager db, DBGeneric dbGeneric, JettyServer httpServer,
			EngineHelper engineHelper)
	{
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.httpServer = httpServer;
		this.engineHelper = engineHelper;
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
				EndpointInstance instance = deserializeEndpoint(raw.getName(), raw.getSubType(), 
						raw.getContents(), sql);
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
			dbGeneric.removeObjectsByType(ENDPOINT_OBJECT_TYPE, sql);
			httpServer.undeployAllEndpoints();
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	public byte[] serializeEndpoint(EndpointInstance endpoint)
	{
		try
		{
			EndpointDescription desc = endpoint.getEndpointDescription();
			String state = endpoint.getSerializedConfiguration();
			String jsonDesc = Constants.MAPPER.writeValueAsString(desc);
			ObjectNode root = Constants.MAPPER.createObjectNode();
			root.put("description", jsonDesc);
			root.put("state", state);
			return Constants.MAPPER.writeValueAsBytes(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize JSON endpoint state", e);
		}

	}
	
	public EndpointInstance deserializeEndpoint(String id, String typeId, byte[] serializedState, SqlSession sql)
	{
		try
		{
			EndpointFactory factory = endpointFactoriesReg.getById(typeId);
			if (factory == null)
				throw new IllegalArgumentException("Endpoint type " + typeId + " is unknown");
			JsonNode root = Constants.MAPPER.readTree(serializedState);
			String descriptionJson = root.get("description").asText();
			String state = root.get("state").asText();
			EndpointDescription description = Constants.MAPPER.readValue(descriptionJson, 
					EndpointDescription.class);

			EndpointInstance instance = factory.newInstance();
			List<Map<String, BindingAuthn>> authenticators = 
					getAuthenticators(description.getAuthenticatorSets(), sql);
			instance.initialize(id, httpServer.getUrls()[0],
					description.getContextAddress(), description.getDescription(), 
					description.getAuthenticatorSets(), authenticators, state);
			return instance;
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't deserialize JSON endpoint state", e);
		} catch (IOException e)
		{
			throw new InternalException("Can't deserialize JSON endpoint state", e);
		} catch (WrongArgumentException e)
		{
			throw new InternalException("Can't deserialize JSON endpoint state - some authenticator(s) " +
					"used in the endpoint are not available", e);
		}
	}
	
	public List<Map<String, BindingAuthn>> getAuthenticators(List<AuthenticatorSet> authn, SqlSession sql) 
			throws WrongArgumentException
	{
		List<Map<String, BindingAuthn>> ret = new ArrayList<Map<String, BindingAuthn>>(authn.size());
		for (AuthenticatorSet aSet: authn)
		{
			Set<String> authenticators = aSet.getAuthenticators();
			Map<String, BindingAuthn> aImpls = new HashMap<String, BindingAuthn>();
			for (String authenticator: authenticators)
			{
				AuthenticatorImpl authImpl = engineHelper.getAuthenticator(authenticator, sql);
				aImpls.put(authenticator, authImpl.getRetrieval());
			}
			ret.add(aImpls);
		}
		return ret;
	}
}
