/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

public class RemoteMetadataServiceTest
{
	private ExecutorsService executorsService;
	private MetadataDownloader downloader;
	
	@Before
	public void init() throws Exception
	{
		executorsService = mock(ExecutorsService.class);
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		when(executorsService.getService()).thenReturn(pool);
		downloader = mock(MetadataDownloader.class);
		when(downloader.getFresh("url", null)).thenAnswer((a) -> {
			String xml = IOUtils.toString(new FileInputStream("src/test/resources/unity-as-sp-meta.xml"));
			return EntitiesDescriptorDocument.Factory.parse(xml);
		});
	}
	
	@Test
	public void shouldCreateHandlerForFirstConsumer()
	{
		RemoteMetadataServiceImpl service = new RemoteMetadataServiceImpl(executorsService, downloader);
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		String key = service.preregisterConsumer("url");
		service.registerConsumer(key, 100, null, (m,id) -> gotEvent.set(true));
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
	}

	@Test
	public void shouldCreateHandlerFor2ndConsumerOtherURL()
	{
		RemoteMetadataServiceImpl service = new RemoteMetadataServiceImpl(executorsService, downloader);
		
		String key = service.preregisterConsumer("url");
		service.registerConsumer(key, 100, null, (m,id) -> {});
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		String key2 = service.preregisterConsumer("url2");
		service.registerConsumer(key2, 100, null, (m,id) -> gotEvent.set(true));
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
	}

	@Test
	public void shouldReuseHandlerFor2ndConsumerSameURL() throws Exception
	{
		RemoteMetadataServiceImpl service = new RemoteMetadataServiceImpl(executorsService,
				downloader);
		when(downloader.getCached("url")).thenAnswer((a) -> {
			String xml = IOUtils.toString(new FileInputStream("src/test/resources/unity-as-sp-meta.xml"));
			return Optional.of(EntitiesDescriptorDocument.Factory.parse(xml));
		});
		
		String key = service.preregisterConsumer("url");
		service.registerConsumer(key, 200, null, (m,id) -> {});
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		String key2 = service.preregisterConsumer("url");
		service.registerConsumer(key2, 200, null, (m,id) -> gotEvent.set(true));
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
		verify(downloader, atMost(1)).getFresh(ArgumentMatchers.anyString(), ArgumentMatchers.any());
	}

	@Test
	public void unregistredConsumerIsRemovedFromHandler() throws InterruptedException
	{
		RemoteMetadataServiceImpl service = new RemoteMetadataServiceImpl(executorsService, downloader);
		
		AtomicInteger gotEvent = new AtomicInteger(0);
		String id = service.preregisterConsumer("url");
		service.registerConsumer(id, 25, null, (m,cid) -> gotEvent.incrementAndGet());
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get()>0);
		service.unregisterConsumer(id);
		int events = gotEvent.get();
		Thread.sleep(100);
		assertThat(events, is(gotEvent.get()));
	}
}
