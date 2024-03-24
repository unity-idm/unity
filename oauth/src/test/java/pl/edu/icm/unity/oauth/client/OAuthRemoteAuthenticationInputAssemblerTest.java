/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata.Protocol;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.GoogleProviderProperties;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthDiscoveryMetadataCache;

@RunWith(MockitoJUnitRunner.class)
public class OAuthRemoteAuthenticationInputAssemblerTest
{
	@Mock
	private OAuthDiscoveryMetadataCache metadataManager;
	@Mock
	private PKIManagement pki;

	@Test
	public void shouldSetRemoteAuthnMetadataInAuthnResult()
	{
		OAuthRemoteAuthenticationInputAssembler assembler = new OAuthRemoteAuthenticationInputAssembler(
				metadataManager);

		RedirectedAuthnState baseAuthnContext = new RedirectedAuthnState(
				new AuthenticationStepContext(null, null, null, null, null), null, null, null,
				AuthenticationTriggeringContext.authenticationTriggeredFirstFactor());

		OAuthContext oAuthContext = new OAuthContext(baseAuthnContext);

		AttributeFetchResult attributes = new AttributeFetchResult(
				Map.of("acr", List.of("acrV"), "iss", List.of("issV")),
				Map.of("acr", List.of("acrV"), "iss", List.of("issV")));
		Properties properties = new Properties();
		properties.setProperty("prefix.clientId", "id");
		properties.setProperty("prefix.clientSecret", "sec");

		CustomProviderProperties customProviderProperties = new GoogleProviderProperties(properties, "prefix.", pki);

		RemotelyAuthenticatedInput convertInput = assembler.convertInput(customProviderProperties, oAuthContext,
				attributes, true);

		assertThat(convertInput.getRemoteAuthnMetadata()
				.protocol).isEqualTo(Protocol.OIDC);
		assertThat(convertInput.getRemoteAuthnMetadata()
				.remoteIdPId).isEqualTo("issV");
		assertThat(convertInput.getRemoteAuthnMetadata()
				.classReferences
				.get(0)).isEqualTo("acrV");
	}
}
