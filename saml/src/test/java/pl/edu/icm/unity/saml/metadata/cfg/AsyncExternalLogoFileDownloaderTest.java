/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.files.logo.RemoteLogoCacheDownloader;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncExternalLogoFileDownloaderTest
{
	@Test
	public void shouldConvertMetadataAndDelegateOnEveryInvocationWithoutBlockingConcurrentCalls()
	{
		MetadataToSPConfigConverter metadataConverter = mock(MetadataToSPConfigConverter.class);
		RemoteLogoCacheDownloader remoteLogoCacheDownloader = mock(RemoteLogoCacheDownloader.class);
		when(remoteLogoCacheDownloader.downloadLogoFilesAsync(any(), any(), any(), any()))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(metadataConverter.convertToTrustedIdPs(any(), any())).thenReturn(new TrustedIdPs(Set.of()));

		AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader = new AsyncExternalLogoFileDownloader(
				metadataConverter,
				remoteLogoCacheDownloader
		);

		EntitiesDescriptorDocument entitiesDescriptorDocument = mock(EntitiesDescriptorDocument.class);
		EntitiesDescriptorType entitiesDescriptorType = mock(EntitiesDescriptorType.class);

		when(entitiesDescriptorDocument.getEntitiesDescriptor()).thenReturn(entitiesDescriptorType);
		when(entitiesDescriptorType.getID()).thenReturn("federationId");

		asyncExternalLogoFileDownloader.downloadLogoFilesAsync(entitiesDescriptorDocument, null);
		asyncExternalLogoFileDownloader.downloadLogoFilesAsync(entitiesDescriptorDocument, null);

		verify(metadataConverter, times(2)).convertToTrustedIdPs(any(), any());
		verify(remoteLogoCacheDownloader, times(2)).downloadLogoFilesAsync(any(), any(), any(), any());
	}
}
