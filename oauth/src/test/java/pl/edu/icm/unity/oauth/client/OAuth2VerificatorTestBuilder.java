/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.oauth.client.config.OAuthClientConfigurationParser;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationMetadataManager;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationProvidersManager;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthDiscoveryMetadataCache;

class OAuth2VerificatorTestBuilder
{
	private PKIManagement pkiManagement = mock(PKIManagement.class);

	OAuth2VerificatorTestBuilder withPkiManagement(PKIManagement pki)
	{
		this.pkiManagement = pki;
		return this;
	}

	OAuth2Verificator build()
	{
		try
		{
			AdvertisedAddressProvider addrProvider = mock(AdvertisedAddressProvider.class);
			when(addrProvider.get()).thenReturn(URI.create("https://unity.example.com").toURL());

			SharedEndpointManagement sharedEndpoint = mock(SharedEndpointManagement.class);
			when(sharedEndpoint.getBaseContextPath()).thenReturn("/unitygw");

			return new OAuth2Verificator(
					addrProvider,
					sharedEndpoint,
					mock(OAuthContextsManagement.class),
					pkiManagement,
					mock(RemoteAuthnResultTranslator.class),
					mock(OAuthDiscoveryMetadataCache.class),
					mock(OAuthRemoteAuthenticationInputAssembler.class),
					mock(OAuthFederationMetadataManager.class),
					mock(OAuthFederationProvidersManager.class),
					mock(OAuthClientConfigurationParser.class));
		} catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
