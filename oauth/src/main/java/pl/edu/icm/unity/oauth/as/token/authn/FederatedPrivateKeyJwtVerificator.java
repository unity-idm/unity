/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.federation.FederatedOAuthClientService;
import pl.edu.icm.unity.oauth.as.federation.OAuthASFederationConfig;
import pl.edu.icm.unity.oauth.as.federation.FederatedOAuthClientService.FederatedClientResolution;

@PrototypeComponent
public class FederatedPrivateKeyJwtVerificator extends AbstractVerificator implements ClientAssertionExchange
{
	public static final String NAME = "private-key-jwt-federated";
	public static final String DESC = "Verifies OAuth2 client JWT assertions using OpenID Federation";
	private static final ResolvableError GENERIC_ERROR =
			new ResolvableError("FederatedPrivateKeyJwtVerificator.invalidAssertion");
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, FederatedPrivateKeyJwtVerificator.class);

	private final OAuthEndpointsCoordinator coordinator;
	private final FederatedOAuthClientService federationClientService;
	private final JwtClientAssertionVerifier jwtVerifier = new JwtClientAssertionVerifier();

	@Autowired
	public FederatedPrivateKeyJwtVerificator(OAuthEndpointsCoordinator coordinator,
			FederatedOAuthClientService federationClientService)
	{
		super(NAME, DESC, ClientAssertionExchange.ID);
		this.coordinator = coordinator;
		this.federationClientService = federationClientService;
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "{}";
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		federationClientService.invalidateChainCache();
	}

	@Override
	public AuthenticationResult verifyClientAssertion(String clientAssertion, URI tokenEndpointUri)
	{
		OAuthASFederationConfig federationConfig = coordinator.getFederationConfig(tokenEndpointUri.toString())
				.orElse(null);
		if (federationConfig == null || !federationConfig.membershipEnabled())
		{
			log.debug("Federation membership not enabled for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
		if (federationConfig.trustAnchorId() == null || federationConfig.trustAnchorJwks() == null)
		{
			log.warn("Federation trust anchor not configured for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		SignedJWT jwt;
		try
		{
			jwt = SignedJWT.parse(clientAssertion);
		} catch (Exception e)
		{
			log.debug("Failed to parse client_assertion JWT", e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		String clientId;
		try
		{
			clientId = jwt.getJWTClaimsSet().getSubject();
			if (clientId == null)
			{
				log.debug("client_assertion JWT has no sub claim");
				return LocalAuthenticationResult.failed(GENERIC_ERROR);
			}
		} catch (Exception e)
		{
			log.debug("Failed to read claims from client_assertion JWT", e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		FederatedClientResolution resolution;
		try
		{
			resolution = federationClientService.resolveAndRegister(clientId, federationConfig);
		} catch (Exception e)
		{
			log.info("Failed to resolve/register federation client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		try
		{
			jwtVerifier.verifyJwt(jwt, resolution.jwks(), tokenEndpointUri, clientId);
		} catch (AuthenticationException e)
		{
			log.info("JWT assertion verification failed for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		AuthenticatedEntity ae = new AuthenticatedEntity(resolution.entityId(), clientId, null);
		return LocalAuthenticationResult.successful(ae, getAuthenticationMethod());
	}

	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.UNKNOWN;
	}

	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<FederatedPrivateKeyJwtVerificator> factory)
		{
			super(NAME, DESC, factory);
		}
	}
}
