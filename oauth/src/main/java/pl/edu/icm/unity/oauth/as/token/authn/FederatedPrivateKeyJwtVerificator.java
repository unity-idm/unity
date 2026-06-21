/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
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
	private final ClientAssertionVerificationFlow verificationFlow = new ClientAssertionVerificationFlow();

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
		Optional<OAuthEndpointsCoordinator.FederationConfigEntry> configEntry =
				coordinator.findFederationConfigByPath(tokenEndpointUri.getPath());
		if (configEntry.isEmpty() || !configEntry.get().config().membershipEnabled())
		{
			log.debug("Federation membership not enabled for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
		OAuthASFederationConfig federationConfig = configEntry.get().config();
		URI canonicalUri = URI.create(configEntry.get().canonicalUrl());
		if (federationConfig.trustAnchorId() == null || federationConfig.trustAnchorJwks() == null)
		{
			log.warn("Federation trust anchor not configured for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
		return verificationFlow.verify(clientAssertion, canonicalUri, GENERIC_ERROR,
				clientId -> resolveFederatedClient(clientId, federationConfig));
	}

	private ClientAssertionVerificationFlow.JwksResolution resolveFederatedClient(String clientId,
			OAuthASFederationConfig federationConfig) throws Exception
	{
		try
		{
			FederatedClientResolution resolution = federationClientService.resolveAndRegister(clientId, federationConfig);
			return new ClientAssertionVerificationFlow.JwksResolution(resolution.entityId(), resolution.jwks());
		} catch (Exception e)
		{
			log.info("Failed to resolve/register federation client {}: {}", clientId, e.getMessage());
			throw e;
		}
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
