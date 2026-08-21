/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.device.DeviceCode;
import com.nimbusds.oauth2.sdk.device.UserCode;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * RFC 8628 §3.1/§3.2: device authorization endpoint. Issues a device_code/user_code pair for
 * clients which have the {@link GrantFlow#deviceCode} grant flow enabled.
 */
@Produces("application/json")
public class DeviceAuthorizationResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceAuthorizationResource.class);

	private final OAuthASProperties config;
	private final OAuthEndpointsCoordinator coordinator;
	private final OAuthRequestValidator requestValidator;
	private final EntityManagement identitiesMan;
	private final DeviceCodeRepository deviceCodeRepository;

	public DeviceAuthorizationResource(OAuthASProperties config, OAuthEndpointsCoordinator coordinator,
			OAuthRequestValidator requestValidator, EntityManagement identitiesMan,
			DeviceCodeRepository deviceCodeRepository)
	{
		this.config = config;
		this.coordinator = coordinator;
		this.requestValidator = requestValidator;
		this.identitiesMan = identitiesMan;
		this.deviceCodeRepository = deviceCodeRepository;
	}

	@Path(OAuthTokenEndpoint.DEVICE_AUTHORIZATION_PATH)
	@POST
	public Response deviceAuthorization(@FormParam("client_id") String clientIdParam,
			@FormParam("scope") String scope)
	{
		if (!config.isDeviceGrantEnabled())
			return makeError(OAuth2Error.INVALID_REQUEST, "device grant disabled");

		ResolvedClient client;
		try
		{
			client = resolveClient(clientIdParam);
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Set<GrantFlow> allowedFlows = requestValidator.getAllowedFlows(client.attributes);
		if (!allowedFlows.contains(GrantFlow.deviceCode))
			return makeError(OAuth2Error.INVALID_CLIENT, "device grant flow is not allowed for this client");

		Scope requestedScope = scope == null ? new Scope() : Scope.parse(scope);
		List<RequestedOAuthScope> validScopes = requestValidator.getValidRequestedScopes(client.attributes,
				requestedScope);

		DeviceCode deviceCode = new DeviceCode();
		UserCode userCode = new UserCode();

		OAuthToken token = new OAuthToken();
		token.setClientId(client.entityId);
		token.setClientUsername(client.username);
		token.setClientName(client.name);
		token.setClientType(client.type);
		token.setEffectiveScope(validScopes);
		token.setRequestedScope(requestedScope.toStringList().toArray(String[]::new));
		token.setIssuerUri(config.getValue(OAuthASProperties.ISSUER_URI));

		DeviceCodeToken deviceCodeToken = new DeviceCodeToken();
		deviceCodeToken.setOauthToken(token);
		deviceCodeToken.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		deviceCodeToken.setUserCode(userCode.getValue());
		deviceCodeToken.setCurrentPollInterval(config.getDeviceCodeMinPollInterval());

		Date now = new Date();
		int validity = config.getDeviceCodeValidity();
		Date expiration = new Date(now.getTime() + validity * 1000L);
		try
		{
			deviceCodeRepository.store(deviceCode.getValue(), deviceCodeToken, now, expiration);
		} catch (Exception e)
		{
			log.error("Can not store the device code", e);
			return makeError(OAuth2Error.SERVER_ERROR, "internal error");
		}

		String issuerUri = config.getValue(OAuthASProperties.ISSUER_URI);
		URI verificationUri;
		try
		{
			verificationUri = toURI(coordinator.getDeviceSignInEndpoint(issuerUri));
		} catch (IllegalArgumentException e)
		{
			log.error("Can not resolve the device sign-in endpoint URL", e);
			return makeError(OAuth2Error.SERVER_ERROR, "device sign-in endpoint is not deployed");
		}
		URI verificationUriComplete = UriBuilder.fromUri(verificationUri)
				.queryParam("user_code", userCode.getValue())
				.build();

		DeviceAuthorizationSuccessResponse response = new DeviceAuthorizationSuccessResponse(deviceCode, userCode,
				verificationUri, verificationUriComplete, validity, config.getDeviceCodeMinPollInterval(), null);
		return toResponse(Response.ok(response.toJSONObject().toJSONString()));
	}

	private ResolvedClient resolveClient(String clientIdParam) throws OAuthErrorException
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		if (loginSession != null)
		{
			long entityId = loginSession.getEntityId();
			String username = loginSession.getAuthenticatedIdentities().iterator().next();
			EntityParam clientEntity = new EntityParam(entityId);
			Map<String, AttributeExt> attributes = getAttributes(clientEntity, username);
			return new ResolvedClient(entityId, username, getClientName(attributes), getClientType(attributes),
					attributes);
		}

		if (clientIdParam == null)
			throw new OAuthErrorException(makeError(OAuth2Error.INVALID_REQUEST, "client_id is required"));

		EntityParam clientEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, clientIdParam));
		Entity entity;
		try
		{
			entity = identitiesMan.getEntity(clientEntity);
		} catch (Exception e)
		{
			throw new OAuthErrorException(makeError(OAuth2Error.INVALID_CLIENT, "unknown client"));
		}

		Map<String, AttributeExt> attributes = getAttributes(clientEntity, clientIdParam);
		ClientType clientType = getClientType(attributes);
		if (clientType == ClientType.CONFIDENTIAL)
			throw new OAuthErrorException(
					makeError(OAuth2Error.INVALID_CLIENT, "confidential client must authenticate"));

		return new ResolvedClient(entity.getId(), clientIdParam, getClientName(attributes), clientType, attributes);
	}

	private Map<String, AttributeExt> getAttributes(EntityParam clientEntity, String client)
			throws OAuthErrorException
	{
		try
		{
			requestValidator.validateGroupMembership(clientEntity, client);
			return requestValidator.getAttributesNoAuthZ(clientEntity);
		} catch (OAuthValidationException e)
		{
			throw new OAuthErrorException(makeError(OAuth2Error.INVALID_CLIENT, e.getMessage()));
		}
	}

	private String getClientName(Map<String, AttributeExt> attributes)
	{
		AttributeExt nameA = attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME);
		return nameA == null ? null : (String) nameA.getValues().get(0);
	}

	private ClientType getClientType(Map<String, AttributeExt> attributes)
	{
		AttributeExt typeA = attributes.get(OAuthSystemAttributesProvider.CLIENT_TYPE);
		return typeA == null ? ClientType.CONFIDENTIAL : ClientType.valueOf(typeA.getValues().get(0).toString());
	}

	private static class ResolvedClient
	{
		final long entityId;
		final String username;
		final String name;
		final ClientType type;
		final Map<String, AttributeExt> attributes;

		ResolvedClient(long entityId, String username, String name, ClientType type,
				Map<String, AttributeExt> attributes)
		{
			this.entityId = entityId;
			this.username = username;
			this.name = name;
			this.type = type;
			this.attributes = attributes;
		}
	}
}
