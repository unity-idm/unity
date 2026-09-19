/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.files.logo.RemoteLogoCacheDownloader;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderKey;
import pl.edu.icm.unity.oauth.client.federation.FederationEntityToProviderConverter.FederationProvider;

/**
 * Prefetches and caches on local disk the logos of federation OPs, mirroring the SAML federation
 * IdP logo cache ({@code pl.edu.icm.unity.saml.metadata.cfg.AsyncExternalLogoFileDownloader}).
 */
@Component
public class OAuthFederationLogoDownloader
{
	public static final String CACHE_GROUP = "oauthFederationLogos";

	private final RemoteLogoCacheDownloader remoteLogoCacheDownloader;

	public OAuthFederationLogoDownloader(RemoteLogoCacheDownloader remoteLogoCacheDownloader)
	{
		this.remoteLogoCacheDownloader = remoteLogoCacheDownloader;
	}

	/**
	 * @param federationId namespace of the caller's federation - always used, even when
	 * {@code providers} is empty or none of them have an icon, so that a refresh removing the last
	 * logo (or all providers) still triggers {@link RemoteLogoCacheDownloader}'s cleanup and doesn't
	 * leave stale files behind for a namespace that's no longer referenced.
	 */
	public void downloadLogoFilesAsync(String federationId, List<FederationProvider> providers, String httpsTruststore)
	{
		Map<OAuthProviderKey, Map<String, String>> logosByKeyAndLocale = providers.stream()
				.filter(p -> p.config().iconUrl() != null)
				.collect(Collectors.toMap(p -> p.config().key(), p -> p.config().iconUrl().getMap(), (a, b) -> a));

		remoteLogoCacheDownloader.downloadLogoFilesAsync(CACHE_GROUP, federationId, logosByKeyAndLocale, httpsTruststore);
	}

	/**
	 * Removes all cached logos of a federation that's no longer configured (e.g. its authenticator was
	 * removed), since no future refresh will run for it to notice via an empty {@code providers} list.
	 */
	public void invalidateNamespace(String federationId)
	{
		remoteLogoCacheDownloader.downloadLogoFilesAsync(CACHE_GROUP, federationId, Map.of(), null);
	}
}
