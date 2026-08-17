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

	public void downloadLogoFilesAsync(List<FederationProvider> providers, String httpsTruststore)
	{
		Map<String, Map<OAuthProviderKey, Map<String, String>>> logosByFederation = providers.stream()
				.filter(p -> p.config().iconUrl() != null && p.config().federationId() != null)
				.collect(Collectors.groupingBy(p -> p.config().federationId(),
						Collectors.toMap(p -> p.config().key(), p -> p.config().iconUrl().getMap())));

		logosByFederation.forEach((federationId, logosByKeyAndLocale) ->
				remoteLogoCacheDownloader.downloadLogoFilesAsync(CACHE_GROUP, federationId, logosByKeyAndLocale, httpsTruststore));
	}
}
