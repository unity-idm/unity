/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.RealmsManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.registries.EndpointFactoriesRegistry;
import pl.edu.icm.unity.server.utils.I18nStringJsonUtil;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@link EndpointInstance}
 * @author K. Benedyczak
 */
@Component
public class EndpointHandler extends DefaultEntityHandler<EndpointInstance>
{
	public static final String ENDPOINT_OBJECT_TYPE = "endpointDefinition";
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private AuthenticatorLoader authnLoader;
	private RealmsManagement realmsManagement;
	
	@Autowired
	public EndpointHandler(ObjectMapper jsonMapper, EndpointFactoriesRegistry endpointFactoriesReg,
			AuthenticatorLoader authnLoader, 
			@Qualifier("insecure") RealmsManagement realmsManagement)
	{
		super(jsonMapper, ENDPOINT_OBJECT_TYPE, EndpointInstance.class);
		this.endpointFactoriesReg = endpointFactoriesReg;
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
			List<AuthenticationOptionDescription> authenticationOptions = desc.getAuthenticatorSets();
			ArrayNode authns = descNode.withArray("authenticationOptions");
			for (AuthenticationOptionDescription authenticationOption: authenticationOptions)
				authns.add(authenticationOption.toJson());
			descNode.put("contextAddress", desc.getContextAddress());
			descNode.put("description", desc.getDescription());
			descNode.put("id", desc.getId());
			descNode.set("displayedName", I18nStringJsonUtil.toJson(desc.getDisplayedName()));
			descNode.put("realmName", desc.getRealm().getName());
			descNode.put("typeName", desc.getType().getName());
			root.put("state", state);
			byte[] serialized = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(desc.getId(), 
					serialized, getType(), desc.getType().getName());
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize JSON endpoint state", e);
		}
	}

	private AuthenticationRealm getDefaultRealm()
	{
		return new AuthenticationRealm("DEFAULT_AUTHN_REALM", 
				"This ralm is set for endpoints which were deployed in "
				+ "server version without realms support. Please use a regular realm instead.", 
				5, 10, -1, 30*60);
	}



	private List<AuthenticationOptionDescription> parseAuthenticationOptions(JsonNode authnSetsNode) 
			throws IOException
	{
		ArrayNode authns = (ArrayNode) authnSetsNode;
		List<AuthenticationOptionDescription> aDescs = new ArrayList<>(authns.size());
		for (JsonNode node: authns)
			aDescs.add(new AuthenticationOptionDescription((ObjectNode) node));
		return aDescs;
	}
	
	@Override
	public EndpointInstance fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			EndpointFactory factory = endpointFactoriesReg.getById(blob.getSubType());
			if (factory == null)
				throw new IllegalArgumentException("Endpoint type " + blob.getSubType() + 
						" is unknown");
			JsonNode root = jsonMapper.readTree(blob.getContents());
			ObjectNode descriptionJson = (ObjectNode) root.get("description");
			String state = root.get("state").asText();
			EndpointDescription description = new EndpointDescription();
			
			if (descriptionJson.has("authenticationOptions"))
				description.setAuthenticatorSets(parseAuthenticationOptions(
						descriptionJson.get("authenticationOptions")));
			else
				description.setAuthenticatorSets(
						AuthenticationOptionDescription.parseLegacyAuthenticatorSets(
						descriptionJson.get("authenticatorSets")));
			
			description.setContextAddress(descriptionJson.get("contextAddress").asText());
			if (descriptionJson.has("description"))
				description.setDescription(descriptionJson.get("description").asText());
			description.setId(descriptionJson.get("id").asText());
			if (descriptionJson.has("displayedName"))
				description.setDisplayedName(I18nStringJsonUtil.fromJson(
						descriptionJson.get("displayedName")));
			else
				description.setDisplayedName(new I18nString(description.getId()));
			
			AuthenticationRealm realm = descriptionJson.has("realmName") ? 
					realmsManagement.getRealm(descriptionJson.get("realmName").asText()) :
					getDefaultRealm();
				
			description.setRealm(realm);
			description.setType(factory.getDescription());

			EndpointInstance instance = factory.newInstance();
			List<AuthenticationOption> authenticators = 
					authnLoader.getAuthenticators(description.getAuthenticatorSets(), sql);
			instance.initialize(description, authenticators, state);
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
