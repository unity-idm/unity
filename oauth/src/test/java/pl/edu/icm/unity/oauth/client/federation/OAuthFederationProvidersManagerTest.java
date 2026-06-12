/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.client.config.FederationConfig;
import pl.edu.icm.unity.oauth.client.config.FederationProviderDefaults;
import pl.edu.icm.unity.oauth.client.config.OAuthClientConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderKey;
import pl.edu.icm.unity.oauth.client.config.OAuthProviders;
import pl.edu.icm.unity.oauth.client.federation.FederationEntityToProviderConverter.FederationProvider;

@ExtendWith(MockitoExtension.class)
class OAuthFederationProvidersManagerTest
{
	private static final String AUTHENTICATOR_ID = "myAuthenticator";
	private static final String CLIENT_ID = "https://rp.example.com";
	private static final String TRUST_ANCHOR = "https://anchor.example.com";
	private static final TranslationProfile PROFILE =
			TranslationProfileGenerator.generateIncludeInputProfile("sys:oidc");

	@Mock
	OAuthFederationService federationService;
	@Mock
	FederationEntityToProviderConverter converter;

	OAuthFederationProvidersManager manager;

	@BeforeEach
	void setUp()
	{
		manager = new OAuthFederationProvidersManager(federationService, converter);
	}

	@Test
	void shouldReturnEmptyProvidersForUnknownAuthenticator()
	{
		OAuthProviders result = manager.getCombinedProviders("unknown");

		assertThat(result.getAll()).isEmpty();
	}

	@Test
	void shouldReturnStaticProvidersWhenFederationDisabled()
	{
		OAuthProviderConfiguration staticProvider = buildStaticProvider("static1");
		OAuthClientConfiguration config = configBuilder()
				.withFederation(disabledFederation())
				.withProviders(new OAuthProviders(List.of(staticProvider)))
				.build();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		OAuthProviders result = manager.getCombinedProviders(AUTHENTICATOR_ID);
		assertThat(result.getAll()).containsExactly(staticProvider);
	}

	@Test
	void shouldNotRegisterConsumerWhenFederationDisabled()
	{
		OAuthClientConfiguration config = configBuilder()
				.withFederation(disabledFederation())
				.withProviders(new OAuthProviders(List.of()))
				.build();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		verify(federationService, never()).preregisterConsumer(anyString());
		verify(federationService, never()).registerConsumer(anyString(), any(), any(), any());
	}

	@Test
	void shouldNotRegisterConsumerWhenTrustAnchorIdIsNull()
	{
		OAuthClientConfiguration config = configBuilder()
				.withFederation(FederationConfig.builder().withEnabled(true).withTrustAnchorId(null)
						.withMetadataValidity(3600).build())
				.withProviders(new OAuthProviders(List.of()))
				.build();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		verify(federationService, never()).preregisterConsumer(anyString());
	}

	@Test
	void shouldNotRegisterConsumerWhenTrustAnchorJwksIsNull()
	{
		OAuthClientConfiguration config = configBuilder()
				.withFederation(FederationConfig.builder().withEnabled(true).withTrustAnchorId(TRUST_ANCHOR)
						.withMetadataValidity(3600).build())
				.withProviders(new OAuthProviders(List.of()))
				.build();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		verify(federationService, never()).preregisterConsumer(anyString());
	}

	@Test
	void shouldRegisterConsumerWhenFederationEnabled()
	{
		when(federationService.preregisterConsumer(TRUST_ANCHOR)).thenReturn("consumer-1");
		OAuthClientConfiguration config = federationEnabledConfig();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		verify(federationService).preregisterConsumer(TRUST_ANCHOR);
		verify(federationService).registerConsumer(eq("consumer-1"), any(Duration.class),
				any(OAuthFederationConfig.class), any());
	}

	@Test
	void shouldMergeFederationProvidersWithStaticOnUpdate()
	{
		OAuthProviderConfiguration staticProvider = buildStaticProvider("static1");
		OAuthProviderConfiguration fedProvider = buildFederationProvider("fed1");
		when(federationService.preregisterConsumer(TRUST_ANCHOR)).thenReturn("consumer-1");
		when(converter.convert(any(), any(), any(), anyBoolean(), any(), any()))
				.thenReturn(List.of(new FederationProvider(fedProvider, Instant.now().plusSeconds(3600))));

		OAuthClientConfiguration config = configBuilder()
				.withFederation(enabledFederation())
				.withFederationProviderDefaults(FederationProviderDefaults.builder().withTranslationProfile(PROFILE).build())
				.withProviders(new OAuthProviders(List.of(staticProvider)))
				.build();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		BiConsumer<List<TrustChain>, String> callback = captureCallback();
		callback.accept(List.of(), "consumer-1");

		OAuthProviders result = manager.getCombinedProviders(AUTHENTICATOR_ID);
		assertThat(result.getKeys()).containsExactlyInAnyOrder(
				staticProvider.key, fedProvider.key);
	}

	@Test
	void shouldFilterExpiredFederationProviders()
	{
		OAuthProviderConfiguration expiredFedProvider = buildFederationProvider("fed-expired");
		when(federationService.preregisterConsumer(TRUST_ANCHOR)).thenReturn("consumer-1");
		when(converter.convert(any(), any(), any(), anyBoolean(), any(), any()))
				.thenReturn(List.of(new FederationProvider(expiredFedProvider,
						Instant.now().minusSeconds(1))));

		OAuthClientConfiguration config = federationEnabledConfig();
		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		BiConsumer<List<TrustChain>, String> callback = captureCallback();
		callback.accept(List.of(), "consumer-1");

		OAuthProviders result = manager.getCombinedProviders(AUTHENTICATOR_ID);
		assertThat(result.getAll()).isEmpty();
	}

	@Test
	void shouldPreferStaticProviderOverFederationProviderWithSameKey()
	{
		OAuthProviderKey sharedKey = OAuthProviderKey.fromFederationEntity("https://idp.example.com");
		OAuthProviderConfiguration staticProvider = buildProviderWithKey(sharedKey, "static");
		OAuthProviderConfiguration fedProvider = buildProviderWithKey(sharedKey, "federation");
		when(federationService.preregisterConsumer(TRUST_ANCHOR)).thenReturn("consumer-1");
		when(converter.convert(any(), any(), any(), anyBoolean(), any(), any()))
				.thenReturn(List.of(new FederationProvider(fedProvider, Instant.now().plusSeconds(3600))));

		OAuthClientConfiguration config = configBuilder()
				.withFederation(enabledFederation())
				.withFederationProviderDefaults(FederationProviderDefaults.builder().withTranslationProfile(PROFILE).build())
				.withProviders(new OAuthProviders(List.of(staticProvider)))
				.build();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);
		BiConsumer<List<TrustChain>, String> callback = captureCallback();
		callback.accept(List.of(), "consumer-1");

		OAuthProviders result = manager.getCombinedProviders(AUTHENTICATOR_ID);
		assertThat(result.getAll()).hasSize(1);
		assertThat(result.get(sharedKey).name.getDefaultValue()).isEqualTo("static");
	}

	@Test
	void shouldUnregisterOldConsumerOnReconfiguration()
	{
		when(federationService.preregisterConsumer(TRUST_ANCHOR))
				.thenReturn("consumer-1")
				.thenReturn("consumer-2");
		OAuthClientConfiguration config = federationEnabledConfig();

		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);
		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		verify(federationService).unregisterConsumer("consumer-1");
	}

	@Test
	void shouldUnregisterConsumerOnRemoveConfiguration()
	{
		when(federationService.preregisterConsumer(TRUST_ANCHOR)).thenReturn("consumer-1");
		OAuthClientConfiguration config = federationEnabledConfig();
		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);

		manager.removeConfiguration(AUTHENTICATOR_ID);

		verify(federationService).unregisterConsumer("consumer-1");
	}

	@Test
	void shouldReturnEmptyAfterRemoveConfiguration()
	{
		when(federationService.preregisterConsumer(TRUST_ANCHOR)).thenReturn("consumer-1");
		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, federationEnabledConfig());
		manager.removeConfiguration(AUTHENTICATOR_ID);

		OAuthProviders result = manager.getCombinedProviders(AUTHENTICATOR_ID);

		assertThat(result.getAll()).isEmpty();
	}

	@Test
	void shouldIgnoreUpdateForRemovedAuthenticator()
	{
		OAuthProviderConfiguration fedProvider = buildFederationProvider("fed1");
		when(federationService.preregisterConsumer(TRUST_ANCHOR)).thenReturn("consumer-1");
		when(converter.convert(any(), any(), any(), anyBoolean(), any(), any()))
				.thenReturn(List.of(new FederationProvider(fedProvider, Instant.now().plusSeconds(3600))));

		OAuthClientConfiguration config = federationEnabledConfig();
		manager.setConfiguration(AUTHENTICATOR_ID, CLIENT_ID, config);
		BiConsumer<List<TrustChain>, String> callback = captureCallback();
		manager.removeConfiguration(AUTHENTICATOR_ID);

		callback.accept(List.of(), "consumer-1");

		assertThat(manager.getCombinedProviders(AUTHENTICATOR_ID).getAll()).isEmpty();
	}

	// --- helpers ---

	@SuppressWarnings("unchecked")
	private BiConsumer<List<TrustChain>, String> captureCallback()
	{
		ArgumentCaptor<BiConsumer<List<TrustChain>, String>> captor = ArgumentCaptor.forClass(BiConsumer.class);
		verify(federationService).registerConsumer(anyString(), any(), any(), captor.capture());
		return captor.getValue();
	}

	private OAuthClientConfiguration federationEnabledConfig()
	{
		return configBuilder()
				.withFederation(enabledFederation())
				.withFederationProviderDefaults(FederationProviderDefaults.builder().withTranslationProfile(PROFILE).build())
				.withProviders(new OAuthProviders(List.of()))
				.build();
	}

	private OAuthClientConfiguration.Builder configBuilder()
	{
		return OAuthClientConfiguration.builder()
				.withDefaultEnableAssociation(true)
				.withFederation(disabledFederation())
				.withFederationProviderDefaults(FederationProviderDefaults.builder().withTranslationProfile(PROFILE).build())
				.withProviders(new OAuthProviders(List.of()));
	}

	private FederationConfig disabledFederation()
	{
		return FederationConfig.builder().withEnabled(false).withMetadataValidity(3600).build();
	}

	private FederationConfig enabledFederation()
	{
		return FederationConfig.builder().withEnabled(true).withTrustAnchorId(TRUST_ANCHOR)
				.withJwks("{\"keys\":[]}").withMetadataValidity(3600).build();
	}

	private OAuthProviderConfiguration buildStaticProvider(String name)
	{
		return OAuthProviderConfiguration.builder()
				.withKey(OAuthProviderKey.fromConfig("providers." + name + "."))
				.withName(new pl.edu.icm.unity.base.i18n.I18nString(name))
				.withTranslationProfile(PROFILE)
				.build();
	}

	private OAuthProviderConfiguration buildFederationProvider(String entityId)
	{
		return buildProviderWithKey(OAuthProviderKey.fromFederationEntity("https://" + entityId + ".example.com"),
				entityId);
	}

	private OAuthProviderConfiguration buildProviderWithKey(OAuthProviderKey key, String name)
	{
		return OAuthProviderConfiguration.builder()
				.withKey(key)
				.withName(new pl.edu.icm.unity.base.i18n.I18nString(name))
				.withTranslationProfile(PROFILE)
				.build();
	}
}
