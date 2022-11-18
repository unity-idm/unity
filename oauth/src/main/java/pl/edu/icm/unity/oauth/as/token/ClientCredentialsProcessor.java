/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.*;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.access.AccessTokenResource;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.util.*;

/**
 * Process client credentials grant flow. 
 * @author Krzysztof Benedyczak
 */
public class ClientCredentialsProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, AccessTokenResource.class);
	private OAuthRequestValidator requestValidator;
	private IdPEngine idpEngine;
	private OAuthASProperties config;
	
	public ClientCredentialsProcessor(OAuthRequestValidator requestValidator,
			IdPEngine idpEngine, OAuthASProperties config)
	{
		this.requestValidator = requestValidator;
		this.idpEngine = idpEngine;
		this.config = config;
	}

	/**
	 * Validates if currently logged user can use the client credentials grant and if so
	 * fills the internalToken. 
	 * @param internalToken
	 * @param scope
	 * @throws OAuthValidationException
	 */
	public OAuthToken processClientFlowRequest(String scope) throws OAuthValidationException
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		EntityParam clientEntity = new EntityParam(loginSession.getEntityId());
		String client = loginSession.getAuthenticatedIdentities().iterator().next();
		
		requestValidator.validateGroupMembership(clientEntity, client);
		Map<String, AttributeExt> attributes = requestValidator.getAttributesNoAuthZ(clientEntity);
		
		Set<GrantFlow> allowedFlows = requestValidator.getAllowedFlows(attributes);
		if (!allowedFlows.contains(GrantFlow.client))
		{
			throw new OAuthValidationException("The '" + client + 
					"' is not authorized to use the '" + GrantFlow.client + "' grant flow.");
		}
		OAuthToken internalToken = new OAuthToken();
		Set<String> requestedAttributes = establishFlowsAndAttributes(internalToken, scope, attributes);
		
		internalToken.setClientId(loginSession.getEntityId());
		internalToken.setClientUsername(client);
		internalToken.setSubject(client);
		
		AttributeExt clientTypeA = attributes.get(OAuthSystemAttributesProvider.CLIENT_TYPE);
		if (clientTypeA != null)
			internalToken.setClientType(ClientType.valueOf(clientTypeA.getValues().get(0)));
		else
			internalToken.setClientType(ClientType.CONFIDENTIAL);
		
		internalToken.setTokenValidity(config.getIntValue(OAuthASProperties.ACCESS_TOKEN_VALIDITY));
		int maxExtendedValidity = config.isSet(OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY) ?
				config.getIntValue(OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY) : 0;
		internalToken.setMaxExtendedValidity(maxExtendedValidity);
		
		String usersGroup = getUsersGroup(attributes);
		TranslationResult translationResult;
		try
		{
			translationResult = getUserInfo(client, usersGroup);
		} catch (EngineException e)
		{
			log.warn("Can not obtain user info for OAuth in client credentials flow", e);
			throw new OAuthValidationException("Internal error");
		}
		Set<DynamicAttribute> filteredAttributes = OAuthProcessor.filterAttributes(
				translationResult, requestedAttributes);
		UserInfo userInfo = OAuthProcessor.prepareUserInfoClaimSet(client, filteredAttributes);
		internalToken.setUserInfo(userInfo.toJSONObject().toJSONString());
		return internalToken;
	}
	
	private Set<String> establishFlowsAndAttributes(OAuthToken internalToken, String scope, Map<String, AttributeExt> clientAttributes)
	{
		Set<String> requestedAttributes = new HashSet<>();
		if (scope != null && !scope.isEmpty())
		{
			Scope parsed = Scope.parse(scope);
			List<OAuthScope> validRequestedScopes = requestValidator.getValidRequestedScopes(clientAttributes, parsed);
			String[] array = validRequestedScopes.stream().
					map(si -> si.name).
					toArray(String[]::new);
			internalToken.setEffectiveScope(array);
			for (OAuthScope si: validRequestedScopes)
				requestedAttributes.addAll(si.attributes);
		}
		return requestedAttributes;
	}
	
	private TranslationResult getUserInfo(String client, String usersGroup) 
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam clientEntity = new EntityParam(ae.getEntityId());
		EntityInGroup clientWithGroup = new EntityInGroup(
				config.getValue(OAuthASProperties.CLIENTS_GROUP), clientEntity);
		
		TranslationResult translationResult = idpEngine.obtainUserInformationWithEnrichingImport(
				clientEntity, 
				usersGroup, 
				config.getOutputTranslationProfile(), 
				client,
				Optional.of(clientWithGroup),
				"OAuth2", 
				GrantType.CLIENT_CREDENTIALS.getValue(),
				true,
				config.getUserImportConfigs());
		return translationResult;
	}
	
	private String getUsersGroup(Map<String, AttributeExt> attributes)
	{
		AttributeExt groupA = attributes.get(OAuthSystemAttributesProvider.PER_CLIENT_GROUP);
		return (groupA != null) ? 
			(String) groupA.getValues().get(0) :
			config.getValue(OAuthASProperties.USERS_GROUP);
	}

}
