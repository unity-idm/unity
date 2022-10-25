/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.junit.Test;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AsyncExternalLogoFileDownloaderTest
{
	@Test
	public void shouldBlockAnotherInvocationOfDownloadLogosWhenLogosAreCurrentlyDownloading()
	{
		UnityServerConfiguration configuration = mock(UnityServerConfiguration.class);
		when(configuration.getValue(eq(UnityServerConfiguration.WORKSPACE_DIRECTORY))).thenReturn(".");
		MessageSource messageSource = mock(MessageSource.class);
		URIAccessService uriAccessService = mock(URIAccessService.class);
		ExecutorsService executorService = mock(ExecutorsService.class);
		MetadataToSPConfigConverter metadataConverter = mock(MetadataToSPConfigConverter.class);

		when(messageSource.getLocale()).thenReturn(new Locale("en"));
		when(executorService.getService()).thenReturn(Executors.newScheduledThreadPool(1));
		AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader = new AsyncExternalLogoFileDownloader(
				configuration,
				messageSource,
				uriAccessService,
				executorService,
				metadataConverter
		);

		EntitiesDescriptorDocument entitiesDescriptorDocument = mock(EntitiesDescriptorDocument.class);
		EntitiesDescriptorType entitiesDescriptorType = mock(EntitiesDescriptorType.class);

		when(entitiesDescriptorDocument.getEntitiesDescriptor()).thenReturn(entitiesDescriptorType);
		when(entitiesDescriptorType.getID()).thenReturn("federationId");

		doAnswer(new AnswersWithDelay( 5000,  new Returns(new TrustedIdPs(Set.of()))) ).when(metadataConverter).convertToTrustedIdPs(any(), any());
		IntStream.range(0, 50).parallel().forEach(x -> asyncExternalLogoFileDownloader.downloadLogoFilesAsync(entitiesDescriptorDocument, null));

		verify(metadataConverter, times(1)).convertToTrustedIdPs(any(), any());
	}
}