/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.logo.RemoteLogoCacheDownloader;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

@Component
public class AsyncExternalLogoFileDownloader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AsyncExternalLogoFileDownloader.class);
	public static final String CACHE_GROUP = "samlIdpLogos";

	private final MetadataToSPConfigConverter converter;
	private final RemoteLogoCacheDownloader remoteLogoCacheDownloader;

	public AsyncExternalLogoFileDownloader(MetadataToSPConfigConverter converter,
			RemoteLogoCacheDownloader remoteLogoCacheDownloader)
	{
		this.converter = converter;
		this.remoteLogoCacheDownloader = remoteLogoCacheDownloader;
	}

	public CompletableFuture<Void> downloadLogoFilesAsync(EntitiesDescriptorDocument entitiesDescriptorDocument, String httpsTruststore)
	{
		String federationId = entitiesDescriptorDocument.getEntitiesDescriptor().getID();
		try
		{
			RemoteMetadataSource metadataSource = RemoteMetadataSource.builder()
					.withTranslationProfile(new TranslationProfile("mock", "description", ProfileType.INPUT, List.of()))
					.withUrl("url")
					.withRefreshInterval(Duration.ZERO)
					.build();
			TrustedIdPs trustedIdPs = converter.convertToTrustedIdPs(entitiesDescriptorDocument, metadataSource);
			log.info("Will download logos for {} IdPs of federation {}", trustedIdPs.getKeys().size(),
					entitiesDescriptorDocument.getEntitiesDescriptor().getName());
			Map<TrustedIdPKey, Map<String, String>> logosByKeyAndLocale = trustedIdPs.getEntrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().logoURI.getMap()));
			return remoteLogoCacheDownloader.downloadLogoFilesAsync(CACHE_GROUP, federationId, logosByKeyAndLocale, httpsTruststore);
		}
		catch (Exception e)
		{
			log.error("This exception occurred when metadata has been converted to TrustedIdPs", e);
			return CompletableFuture.completedFuture(null);
		}
	}
}
