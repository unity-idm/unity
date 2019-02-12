/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.util.AntPathMatcher;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;


/**
 * Validation of initial OAuth request, happens before starting interaction with the redirected user (authN, consent).
 */
class OAuthWebRequestValidator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthWebRequestValidator.class);
	
	private static final Set<ResponseType.Value> KNOWN_RESPONSE_TYPES = Sets.newHashSet(
			ResponseType.Value.CODE, ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN);
	
	private OAuthASProperties oauthConfig;
	private EntityManagement identitiesMan;
	private OAuthRequestValidator baseRequestValidator;
	
	public OAuthWebRequestValidator(OAuthASProperties oauthConfig, 
			EntityManagement identitiesMan,
			AttributesManagement attributesMan)
	{
		this.oauthConfig = oauthConfig;
		this.identitiesMan = identitiesMan;
		this.baseRequestValidator = new OAuthRequestValidator(oauthConfig, identitiesMan, attributesMan);
	}

	/**
	 * Checks if the requested client is defined, if the return URL is valid for the client and 
	 * whether the authorization grant is enabled for the client.
	 * @param context
	 */
	void validate(OAuthAuthzContext context) throws OAuthValidationException
	{
		AuthorizationRequest authzRequest = context.getRequest();
		String client = authzRequest.getClientID().getValue();
		EntityParam clientEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, client)); 
		try
		{
			Entity clientResolvedEntity = identitiesMan.getEntity(clientEntity);
			context.setClientEntityId(clientResolvedEntity.getId());
		} catch (IllegalArgumentException e)
		{
			throw new OAuthValidationException("The client '" + client + "' is unknown");
		} catch (Exception e)
		{
			log.error("Problem retrieving identity of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}

		context.setClientUsername(client);
		
		baseRequestValidator.validateGroupMembership(clientEntity, client);
		Map<String, AttributeExt> attributes = baseRequestValidator.getAttributesNoAuthZ(clientEntity);
		
		AttributeExt allowedUrisA = attributes.get(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI);
		AttributeExt nameA = attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME);
		AttributeExt logoA = attributes.get(OAuthSystemAttributesProvider.CLIENT_LOGO);
		AttributeExt groupA = attributes.get(OAuthSystemAttributesProvider.PER_CLIENT_GROUP);
		
		AttributeExt clientTypeA = attributes.get(OAuthSystemAttributesProvider.CLIENT_TYPE);
		if (clientTypeA != null)
			context.setClientType(ClientType.valueOf(clientTypeA.getValues().get(0)));
		else
			context.setClientType(ClientType.CONFIDENTIAL);

		if (allowedUrisA == null || allowedUrisA.getValues().isEmpty())
			throw new OAuthValidationException("The '" + client + 
					"' has no authorized redirect URI(s) defined");
		Set<String> allowedUris = new LinkedHashSet<>();
		for (Object val: allowedUrisA.getValues())
			allowedUris.add(val.toString());
		
		Set<GrantFlow> allowedFlows = baseRequestValidator.getAllowedFlows(attributes);
		
		validateFlowAndMode(authzRequest, allowedFlows, client, context);
		
		URI redirectionURI = authzRequest.getRedirectionURI();
		
		validateReturnURI(redirectionURI, allowedUris, client, context.getClientType());
		
		if (redirectionURI != null)
		{
			context.setReturnURI(redirectionURI);
		} else
		{
			String configuredUri = allowedUris.iterator().next();
			try
			{
				context.setReturnURI(new URI(configuredUri));
			} catch (URISyntaxException e)
			{
				log.error("The URI configured for the client '" + client + 
						"' is invalid: " + configuredUri, e);
				throw new OAuthValidationException("The URI configured for the client '" + client + 
						"' is invalid: " + configuredUri);
			}
		}

		if (logoA != null)
			context.setClientLogo(logoA);
		
		if (nameA != null)
			context.setClientName(nameA.getValues().get(0));
		
		if (groupA != null)
			context.setUsersGroup(groupA.getValues().get(0));
		else
			context.setUsersGroup(oauthConfig.getValue(OAuthASProperties.USERS_GROUP));
		
		context.setTranslationProfile(oauthConfig.getValue(CommonIdPProperties.TRANSLATION_PROFILE));

		Scope requestedScopes = authzRequest.getScope();
		if (requestedScopes != null)
		{
			List<ScopeInfo> validRequestedScopes = baseRequestValidator
					.getValidRequestedScopes(requestedScopes);
			validRequestedScopes.forEach(si -> context.addEffectiveScopeInfo(si));
			requestedScopes.forEach(si -> context.addRequestedScope(si.getValue()));
		}
		
		if (context.getClientType() == ClientType.PUBLIC)
			validatePKCEIsUsedForCodeFlow(authzRequest, client);
	}

	private void validatePKCEIsUsedForCodeFlow(AuthorizationRequest authzRequest, String client) throws OAuthValidationException
	{
		ResponseType responseType = authzRequest.getResponseType();
		if (responseType.contains(ResponseType.Value.CODE) && authzRequest.getCodeChallenge() == null)
			throw new OAuthValidationException("The public client '" + client + 
				"' must use PKCE Oauth extension for security reasons, while no challenge found");
	}

	private void validateReturnURI(URI redirectionURI, Set<String> allowedUris, String client, 
			ClientType clientType) throws OAuthValidationException
	{
		if (redirectionURI == null)
			return;
		String redirect = redirectionURI.toString();
		
		URI requestedURI;
		try
		{
			requestedURI = new URI(redirect);
		} catch (URISyntaxException e)
		{
			log.debug("Requested URI parsing problem", e);
			throw new OAuthValidationException("The requested return address '" + redirect + 
					"' can not be parsed as URI");
		}
		
		if (clientType == ClientType.PUBLIC)
		{
			if (allowedLoopbackURI(requestedURI, allowedUris))
				return;
			assertPrivateUseURIIsSane(requestedURI);
		}
		
		boolean supportWildcards = oauthConfig.getBooleanValue(OAuthASProperties.ALLOW_FOR_WILDCARDS_IN_ALLOWED_URI);
		
		if (!supportWildcards)
		{
			if (allowedUris.contains(redirect))
				return;
		} else
		{
			AntPathMatcher matcher = new AntPathMatcher();
			for (String allowed: allowedUris)
			{
				if (matcher.match(allowed, redirect))
					return;
			}
		}
		throw new OAuthValidationException("The '" + client + 
				"' requested to use a not registered response redirection URI: " + 
				redirectionURI);
	}
	
	private void assertPrivateUseURIIsSane(URI requestedURI) throws OAuthValidationException
	{
		String scheme = requestedURI.getScheme();
		if (!scheme.equals("http") && !scheme.equals("https"))
			if (!scheme.contains("."))
				throw new OAuthValidationException("The requested return URI "
						+ "seems to be private use URI, but has no mandatory dot in it: '" + scheme + 
						"'");
	}

	private boolean allowedLoopbackURI(URI requestedURI, Set<String> allowedUris) throws OAuthValidationException
	{
		String host = requestedURI.getHost();
		String scheme = requestedURI.getScheme();
		if (host == null || scheme == null)
			return false;
		if (host.equals("127.0.0.1") || host.equals("[::1]") && scheme.equals("http") || scheme.equals("https"))
		{
			for (String allowed: allowedUris)
				if(verifyIfMatchingLoopbackURIs(requestedURI, allowed))
					return true;
		}
		return false;
	}

	/**
	 * allowed uri must be any loopback IP-based URI, and paths must match exactly.
	 */
	private boolean verifyIfMatchingLoopbackURIs(URI requestedURI, String allowed)
	{
		URI allowedURI;
		try
		{
			allowedURI = new URI(allowed);
		} catch (URISyntaxException e)
		{
			log.warn("The configured allowed URI '" + allowed + 
					"' can not be parsed as URI", e);
			return false;
		}
		if (!allowedURI.getScheme().equals("http") && !allowedURI.getScheme().equals("https"))
			return false;
		if (!allowedURI.getHost().equals("127.0.0.1") && !allowedURI.getHost().equals("[::1]"))
			return false;
		if (!allowedURI.getPath().equals(requestedURI.getPath()))
			return false;
		return true;
	}
	
	

	/**
	 * Checks if the response type(s) are understood and in proper combination. Also openid connect mode
	 * is set and checked for consistency with the flow. It is checked if the client can use the requested flow. 
	 * @param authzRequest
	 * @param allowedFlows
	 * @param client
	 * @param context
	 * @throws OAuthValidationException
	 */
	private void validateFlowAndMode(AuthorizationRequest authzRequest, Set<GrantFlow> allowedFlows, 
			String client, OAuthAuthzContext context) throws OAuthValidationException
	{
		ResponseType responseType = authzRequest.getResponseType();
		Scope requestedScopes = authzRequest.getScope();

		context.setOpenIdMode(requestedScopes != null && requestedScopes.contains(OIDCScopeValue.OPENID));

		if (context.isOpenIdMode())
		{
			if (responseType.contains(ResponseType.Value.TOKEN) && responseType.size() == 1)
				throw new OAuthValidationException("The OpenID Connect mode implied by "
						+ "the requested 'openid' scope can not be used with the "
						+ "'token' response type - it makes no sense");
			if (!(authzRequest instanceof AuthenticationRequest))
				throw new OAuthValidationException("The OpenID Connect mode implied by "
						+ "the requested 'openid' scope was used with non OIDC compliant, "
						+ "plain OOAuth request");
		} else
		{
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN))
				throw new OAuthValidationException("The 'openid' scope was not requested and the "
						+ "'id_token' response type was what is an invalid combination");
		}

		
		SetView<ResponseType.Value> diff = Sets.difference(responseType, KNOWN_RESPONSE_TYPES);
		if (!diff.isEmpty())
			throw new OAuthValidationException("The following response type(s) is(are) not supported: " 
					+ diff);
		
		if (responseType.contains(ResponseType.Value.CODE))
		{
			if (responseType.size() == 1)
			{
				context.setFlow(GrantFlow.authorizationCode);
			} else
			{
				context.setFlow(GrantFlow.openidHybrid);
				if (!context.isOpenIdMode())
					throw new OAuthValidationException("The OpenID Connect mode "
							+ "implied by the requested hybrid flow "
							+ "must use the 'openid' scope");
			}
		} else
		{
			context.setFlow(GrantFlow.implicit);
		}
		
		if (!allowedFlows.contains(context.getFlow()))
			throw new OAuthValidationException("The '" + client + 
					"' is not authorized to use the '" + context.getFlow() + "' grant flow.");
	}
}




