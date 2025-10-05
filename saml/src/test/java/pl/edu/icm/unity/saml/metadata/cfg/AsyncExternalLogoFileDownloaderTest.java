/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.ValidableAnswer;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

		when(messageSource.getLocale()).thenReturn(Locale.forLanguageTag("en"));
		when(executorService.getExecutionService()).thenReturn(Executors.newWorkStealingPool(1));
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

		CompletableFuture<Void> waitingTask = new CompletableFuture<>();
		doAnswer(new AnswerAfterFuture(waitingTask,  new Returns(new TrustedIdPs(Set.of())))).when(metadataConverter).convertToTrustedIdPs(any(), any());

		CompletableFuture.allOf(
			CompletableFuture.runAsync(() -> asyncExternalLogoFileDownloader.downloadLogoFilesAsync(entitiesDescriptorDocument, null)),
			CompletableFuture.runAsync(() -> asyncExternalLogoFileDownloader.downloadLogoFilesAsync(entitiesDescriptorDocument, null))
		).thenRun(() -> waitingTask.complete(null));

		verify(metadataConverter, timeout(5000).times(1)).convertToTrustedIdPs(any(), any());
	}

	static class AnswerAfterFuture implements Answer<Object>, ValidableAnswer, Serializable
	{
		private final CompletableFuture<Void> waiter;
		private final Answer<Object> answer;

		public AnswerAfterFuture(CompletableFuture<Void> waiter, Answer<Object> answer) {
			this.waiter = waiter;
			this.answer = answer;
		}

		@Override
		public Object answer(final InvocationOnMock invocation) throws Throwable {
			waiter.join();
			return answer.answer(invocation);
		}

		@Override
		public void validateFor(InvocationOnMock invocation) {
			if (answer instanceof ValidableAnswer) {
				((ValidableAnswer) answer).validateFor(invocation);
				return;
			}
			throw new IllegalArgumentException("Answer have to be instanceof ValidableAnswer");
		}
	}
}