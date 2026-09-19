/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.files.logo.RemoteLogoCacheDownloader;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderKey;
import pl.edu.icm.unity.oauth.client.federation.FederationEntityToProviderConverter.FederationProvider;

class OAuthFederationLogoDownloaderTest
{
	private static final String FEDERATION_ID = "https://anchor.example.com";
	private static final String TRUSTSTORE = "MAIN";

	private final RemoteLogoCacheDownloader remoteLogoCacheDownloader = mock(RemoteLogoCacheDownloader.class);
	private final OAuthFederationLogoDownloader tested = new OAuthFederationLogoDownloader(remoteLogoCacheDownloader);

	@Test
	void shouldPassOnlyProvidersWithIconToRemoteDownloader()
	{
		OAuthProviderConfiguration withIcon = buildProvider("withIcon", "https://op.example.com/logo.png");
		OAuthProviderConfiguration withoutIcon = buildProvider("withoutIcon", null);
		List<FederationProvider> providers = List.of(
				new FederationProvider(withIcon, Instant.now().plusSeconds(3600)),
				new FederationProvider(withoutIcon, Instant.now().plusSeconds(3600)));

		tested.downloadLogoFilesAsync(FEDERATION_ID, providers, TRUSTSTORE);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<OAuthProviderKey, Map<String, String>>> captor = ArgumentCaptor.forClass(Map.class);
		verify(remoteLogoCacheDownloader).downloadLogoFilesAsync(eq(OAuthFederationLogoDownloader.CACHE_GROUP),
				eq(FEDERATION_ID), captor.capture(), eq(TRUSTSTORE));
		assertKeysOnly(captor.getValue(), withIcon.key());
	}

	@Test
	void shouldStillInvokeDownloaderWithEmptyMapWhenNoProviderHasIcon()
	{
		OAuthProviderConfiguration withoutIcon = buildProvider("withoutIcon", null);
		List<FederationProvider> providers = List.of(new FederationProvider(withoutIcon, Instant.now().plusSeconds(3600)));

		tested.downloadLogoFilesAsync(FEDERATION_ID, providers, TRUSTSTORE);

		verify(remoteLogoCacheDownloader).downloadLogoFilesAsync(eq(OAuthFederationLogoDownloader.CACHE_GROUP),
				eq(FEDERATION_ID), eq(Map.of()), eq(TRUSTSTORE));
	}

	@Test
	void shouldStillInvokeDownloaderWithEmptyMapWhenNoProvidersAtAll()
	{
		tested.downloadLogoFilesAsync(FEDERATION_ID, List.of(), TRUSTSTORE);

		verify(remoteLogoCacheDownloader).downloadLogoFilesAsync(eq(OAuthFederationLogoDownloader.CACHE_GROUP),
				eq(FEDERATION_ID), eq(Map.of()), eq(TRUSTSTORE));
	}

	@Test
	void shouldInvalidateNamespaceWithEmptyMap()
	{
		tested.invalidateNamespace(FEDERATION_ID);

		verify(remoteLogoCacheDownloader).downloadLogoFilesAsync(eq(OAuthFederationLogoDownloader.CACHE_GROUP),
				eq(FEDERATION_ID), eq(Map.of()), isNull());
	}

	private void assertKeysOnly(Map<OAuthProviderKey, Map<String, String>> map, OAuthProviderKey... expectedKeys)
	{
		org.assertj.core.api.Assertions.assertThat(map.keySet()).containsExactlyInAnyOrder(expectedKeys);
	}

	private OAuthProviderConfiguration buildProvider(String entityId, String iconUrl)
	{
		return OAuthProviderConfiguration.builder()
				.withKey(OAuthProviderKey.fromFederationEntity("https://" + entityId + ".example.com"))
				.withName(new I18nString(entityId))
				.withIconUrl(iconUrl == null ? null : new I18nString(iconUrl))
				.withFederationId(FEDERATION_ID)
				.build();
	}
}
