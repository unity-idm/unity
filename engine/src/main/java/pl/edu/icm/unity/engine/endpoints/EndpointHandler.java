/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoints;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.RealmsManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.registries.EndpointFactoriesRegistry;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Handler for {@link EndpointInstance}
 * @author K. Benedyczak
 */
@Component
public class EndpointHandler extends DefaultEntityHandler<EndpointInstance>
{
	public static final String ENDPOINT_OBJECT_TYPE = "endpointDefinition";
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private JettyServer httpServer;
	private AuthenticatorLoader authnLoader;
	private RealmsManagement realmsManagement;
	
	@Autowired
	public EndpointHandler(ObjectMapper jsonMapper, EndpointFactoriesRegistry endpointFactoriesReg,
			JettyServer httpServer, AuthenticatorLoader authnLoader, RealmsManagement realmsManagement)
	{
		super(jsonMapper, ENDPOINT_OBJECT_TYPE, EndpointInstance.class);
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.httpServer = httpServer;
		this.authnLoader = authnLoader;
		this.realmsManagement = realmsManagement;
	}

	@Override
	public GenericObjectBean toBlob(EndpointInstance endpoint, SqlSession sql)
	{
		try
		{
			EndpointDescription desc = endpoint.getEndpointDescription();
			String state = endpoint.getSerializedConfiguration();
			ObjectNode root = jsonMapper.createObjectNode();
			ObjectNode descNode = root.putObject("description");
			String authenticators = jsonMapper.writeValueAsString(desc.getAuthenticatorSets());
			descNode.put("authenticatorSets", authenticators);
			descNode.put("contextAddress", desc.getContextAddress());
			descNode.put("description", desc.getDescription());
			descNode.put("id", desc.getId());
			descNode.put("realmName", desc.getRealm().getName());
			descNode.put("typeName", desc.getType().getName());
			root.put("state", state);
			byte[] serialized = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(endpoint.getEndpointDescription().getId(), 
					serialized, getType(), endpoint.getEndpointDescription().getType().getName());
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize JSON endpoint state", e);
		}
	}

	@Override
	public EndpointInstance fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			EndpointFactory factory = endpointFactoriesReg.getById(blob.getSubType());
			if (factory == null)
				throw new IllegalArgumentException("Endpoint type " + blob.getSubType() + " is unknown");
			JsonNode root = jsonMapper.readTree(blob.getContents());
			ObjectNode descriptionJson = (ObjectNode) root.get("description");
			String state = root.get("state").asText();
			EndpointDescription description = new EndpointDescription();
			
			List<AuthenticatorSet> aSet = jsonMapper.readValue(
					descriptionJson.get("authenticatorSets").asText(), 
					new TypeReference<List<AuthenticatorSet>>() {});
			description.setAuthenticatorSets(aSet);
			description.setContextAddress(descriptionJson.get("contextAddress").asText());
			if (descriptionJson.has("description"))
				description.setDescription(descriptionJson.get("description").asText());
			description.setId(descriptionJson.get("id").asText());
			
			AuthenticationRealm realm = descriptionJson.has("realmName") ? 
					realmsManagement.getRealm(descriptionJson.get("realmName").asText()) :
					realmsManagement.getDefaultRealm();
				
			description.setRealm(realm);
			description.setType(factory.getDescription());

			EndpointInstance instance = factory.newInstance();
			List<Map<String, BindingAuthn>> authenticators = 
					authnLoader.getAuthenticators(description.getAuthenticatorSets(), sql);
			instance.initialize(blob.getName(), httpServer.getAdvertisedAddress(),
					description.getContextAddress(), description.getDescription(), 
					description.getAuthenticatorSets(), authenticators, realm, state);
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
		} catch (EngineException e)
		{
			throw new InternalException("Can't deserialize JSON endpoint state - other problem", e);
		}
	}

}
