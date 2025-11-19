/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.Scope;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthParseServlet;

/**
 * Utility class with methods useful for request checking and its mapping to unity 
 * abstraction. Used by {@link OAuthParseServlet} and {@link OAuthTokenEndpoint} in the case of client 
 * credentials grant.
 * 
 * @author K. Benedyczak
 */
public class OAuthRequestValidator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthRequestValidator.class);
	
	protected OAuthASProperties oauthConfig;
	protected EntityManagement identitiesMan;
	protected AttributesManagement attributesMan;
	protected OAuthScopesService scopeService;
	
	public OAuthRequestValidator(OAuthASProperties oauthConfig,
			EntityManagement identitiesMan, AttributesManagement attributesMan, OAuthScopesService scopeService)
	{
		this.oauthConfig = oauthConfig;
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
		this.scopeService = scopeService;
	}

	/**
	 * Checks if the client is a member of the configured OAuth clients group 
	 */
	public void validateGroupMembership(EntityParam clientEntity, String client)
			throws OAuthValidationException
	{
		Collection<String> groups;
		try
		{
			groups = identitiesMan.getGroups(clientEntity).keySet();
		} catch (EngineException e)
		{
			log.error("Problem retrieving groups of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}
		String oauthGroup = oauthConfig.getValue(OAuthASProperties.CLIENTS_GROUP);
		if (!groups.contains(oauthGroup))
			throw new OAuthValidationException("The '" + client + "' is not authorized as OAuth client "
					+ "(not in the clients group)");
	}
	
	public Map<String, AttributeExt> getAttributesNoAuthZ(EntityParam clientEntity) throws OAuthValidationException
	{
		String oauthGroup = oauthConfig.getValue(OAuthASProperties.CLIENTS_GROUP);
		Collection<AttributeExt> attrs;
		try
		{
			attrs = attributesMan.getAllAttributes(clientEntity, true, oauthGroup, null, false);
		} catch (EngineException e)
		{
			log.error("Problem retrieving attributes of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}
		Map<String, AttributeExt> ret = new HashMap<>();
		attrs.stream().forEach(a -> ret.put(a.getName(), a));
		return ret;
	}
	
	public Set<GrantFlow> getAllowedFlows(Map<String, AttributeExt> attributes)
	{
		Set<GrantFlow> allowedFlows = new HashSet<>();
		AttributeExt allowedFlowsA = attributes.get(OAuthSystemAttributesProvider.ALLOWED_FLOWS);
		if (allowedFlowsA == null)
		{
			allowedFlows.add(GrantFlow.authorizationCode);
		} else
		{
			for (Object val: allowedFlowsA.getValues())
				allowedFlows.add(GrantFlow.valueOf(val.toString()));
		}
		return allowedFlows;
	}
	
	public Optional<Set<String>> getAllowedScopes(Map<String, AttributeExt> attributes)
	{
		AttributeExt allowedScopesA = attributes.get(OAuthSystemAttributesProvider.ALLOWED_SCOPES);
		if (allowedScopesA == null)
		{
			return Optional.empty();
		} else
		{
			return Optional.of(Set.copyOf(allowedScopesA.getValues()));
		}
	}

	public List<RequestedOAuthScope> getValidRequestedScopes(Map<String, AttributeExt> clientAttributes, Scope requestedScopes)
	{
		List<OAuthScopeDefinition> scopesDefinedOnServer = scopeService.getActiveScopes(oauthConfig);
		Optional<Set<String>> allowedByClientScopes = getAllowedScopes(clientAttributes);
		Set<String> notAllowedByClient = requestedScopes.stream().map(s -> s.getValue())
				.filter(scope -> (allowedByClientScopes.isPresent() && !allowedByClientScopes.get().contains(scope)))
				.collect(Collectors.toSet());
		if (!notAllowedByClient.isEmpty())
		{
			log.info("Requested scopes not allowed for the client and ignored: %", String.join(",", notAllowedByClient));
		}

		Set<String> notDefinedOnServer = requestedScopes.stream().map(s -> s.getValue())
				.filter(scope -> !scopesDefinedOnServer.stream()
						.filter(serverScope -> serverScope.match(scope)).findAny().isPresent())
				.collect(Collectors.toSet());
		if (!notDefinedOnServer.isEmpty())
		{
			log.info("Requested scopes not available on the endpoint and ignored: "
					+ String.join(",", notDefinedOnServer));
		}

		return requestedScopes.stream()
				.filter(s -> !notDefinedOnServer.contains(s.getValue()) && !notAllowedByClient.contains(s.getValue()))
				.map(s -> toRequestedScope(s.getValue(), scopesDefinedOnServer.stream()
						.filter(serverScope -> serverScope.match(s.getValue()))
						.findFirst()
						.get()))
				.collect(Collectors.toList());		
	}
	
	private RequestedOAuthScope toRequestedScope(String scope, OAuthScopeDefinition serverScope)
	{
		return new RequestedOAuthScope(scope, serverScope); 
	}
	
	@Component
	public static class OAuthRequestValidatorFactory
	{
		private final EntityManagement identitiesMan;
		private final AttributesManagement attributesMan;
		private final OAuthScopesService scopeService;
		
		@Autowired
		public OAuthRequestValidatorFactory(EntityManagement identitiesMan, @Qualifier("insecure") AttributesManagement attributesMan,
				OAuthScopesService scopeService)
		{	
			this.identitiesMan = identitiesMan;
			this.attributesMan = attributesMan;
			this.scopeService = scopeService;
		}
		
		public OAuthRequestValidator getOAuthRequestValidator(OAuthASProperties oauthConfig)
		{
			return new OAuthRequestValidator(oauthConfig, identitiesMan, attributesMan, scopeService);
		}	
	}
}
