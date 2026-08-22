/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;

import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.logo.CachedLogoFileLoader;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderKey;
import pl.edu.icm.unity.oauth.client.federation.OAuthFederationLogoDownloader;

class OAuthProviderLogoLoaderTest
{
	private final CachedLogoFileLoader cachedLogoFileLoader = mock(CachedLogoFileLoader.class);
	private final VaadinLogoImageLoader vaadinLogoImageLoader = mock(VaadinLogoImageLoader.class);
	private final OAuthProviderLogoLoader tested = new OAuthProviderLogoLoader(cachedLogoFileLoader, vaadinLogoImageLoader);
	private final MessageSource msg = mock(MessageSource.class);

	@Test
	void shouldReturnEmptyWhenIconUrlIsNull()
	{
		OAuthProviderConfiguration provider = buildProvider(null, null);

		Optional<Image> result = tested.loadLogo(provider, msg);

		assertThat(result).isEmpty();
	}

	@Test
	void shouldFetchDirectlyForNonFederationProvider()
	{
		OAuthProviderConfiguration provider = buildProvider("https://provider.example.com/logo.png", null);
		LocalOrRemoteResource expected = new LocalOrRemoteResource("https://provider.example.com/logo.png", "");
		when(vaadinLogoImageLoader.loadImageFromUri("https://provider.example.com/logo.png"))
				.thenReturn(Optional.of(expected));

		Optional<Image> result = tested.loadLogo(provider, msg);

		assertThat(result).containsSame(expected);
	}

	@Test
	void shouldFetchDirectlyForFileUriEvenWhenFederationProvider()
	{
		OAuthProviderConfiguration provider = buildProvider("file:/tmp/logo.png", "https://anchor.example.com");
		LocalOrRemoteResource expected = new LocalOrRemoteResource("file:/tmp/logo.png", "");
		when(vaadinLogoImageLoader.loadImageFromUri("file:/tmp/logo.png")).thenReturn(Optional.of(expected));

		Optional<Image> result = tested.loadLogo(provider, msg);

		assertThat(result).containsSame(expected);
		verify(cachedLogoFileLoader, never()).getFile(any(), any(), any(), any());
	}

	@Test
	void shouldReturnPrefetchedLogoForFederationProvider(@TempDir Path tempDir) throws IOException
	{
		Path file = tempDir.resolve("logo.png");
		Files.writeString(file, "png-bytes");
		OAuthProviderConfiguration provider = buildProvider("https://op.example.com/logo.png", "https://anchor.example.com");
		when(cachedLogoFileLoader.getFile(eq(OAuthFederationLogoDownloader.CACHE_GROUP),
				eq("https://anchor.example.com"), eq(provider.key()), any(Locale.class)))
				.thenReturn(Optional.of(file.toFile()));

		UI mockUI = Mockito.mock(UI.class);
		when(mockUI.getId()).thenReturn(Optional.of("uiId"));
		UI.setCurrent(mockUI);

		Optional<Image> result = tested.loadLogo(provider, msg);

		assertThat(result).isPresent();
		verify(vaadinLogoImageLoader, never()).loadImageFromUri(any());
	}

	@Test
	void shouldReturnEmptyOnCacheMissForFederationProviderWithoutFallbackToDirectFetch()
	{
		OAuthProviderConfiguration provider = buildProvider("https://op.example.com/logo.png", "https://anchor.example.com");
		when(cachedLogoFileLoader.getFile(eq(OAuthFederationLogoDownloader.CACHE_GROUP),
				eq("https://anchor.example.com"), eq(provider.key()), any(Locale.class)))
				.thenReturn(Optional.empty());

		Optional<Image> result = tested.loadLogo(provider, msg);

		assertThat(result).isEmpty();
		// security-critical: a federation-declared iconUrl is untrusted, so a cache miss must never
		// fall back to fetching it directly (that would reproduce the SSRF/LFI risk the cache exists to avoid)
		verify(vaadinLogoImageLoader, never()).loadImageFromUri(any());
	}

	@Test
	void shouldReturnEmptyWhenCachedLogoLoaderThrows()
	{
		OAuthProviderConfiguration provider = buildProvider("https://op.example.com/logo.png", "https://anchor.example.com");
		when(cachedLogoFileLoader.getFile(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));

		Optional<Image> result = tested.loadLogo(provider, msg);

		assertThat(result).isEmpty();
	}

	private OAuthProviderConfiguration buildProvider(String iconUrl, String federationId)
	{
		return OAuthProviderConfiguration.builder()
				.withKey(federationId == null ? OAuthProviderKey.fromConfig("providers.p1.")
						: OAuthProviderKey.fromFederationEntity("https://op.example.com"))
				.withName(new I18nString("provider"))
				.withIconUrl(iconUrl == null ? null : new I18nString(iconUrl))
				.withFederationId(federationId)
				.build();
	}
}
