/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AttributesCacheRefreshServiceTest
{
	@Mock
	private BulkGroupQueryService bulkGroupQueryService;
	@Mock
	private AttributesCacheDAO attributesCacheDAO;
	@Mock
	private AttributesCachePendingDAO attributesCachePendingDAO;
	@Mock
	private TransactionalRunner tx;
	@Mock
	private ExecutorsService executorsService;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@AfterEach
	public void shutdownExecutor()
	{
		executor.shutdownNow();
	}

	private AttributesCacheRefreshService create()
	{
		when(executorsService.getExecutionService()).thenReturn(executor);
		when(tx.runInTransactionRet(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
		return new AttributesCacheRefreshService(bulkGroupQueryService, attributesCacheDAO,
				attributesCachePendingDAO, tx, executorsService);
	}

	@Test
	public void shouldNotRunBeforeStart()
	{
		AttributesCacheRefreshService service = create();

		assertThat(service.isRunning()).isFalse();
		service.wakeUp();
	}

	@Test
	public void stopShouldAwaitThreadTermination()
	{
		AttributesCacheRefreshService service = create();
		service.start();
		AttributesCacheRefreshThread thread = service.refreshThread;
		assertThat(service.isRunning()).isTrue();
		assertThat(thread.isAlive()).isTrue();

		service.stop();

		assertThat(service.isRunning()).isFalse();
		assertThat(thread.isAlive()).isFalse();
		service.wakeUp();
	}

	@Test
	public void shouldBeRestartableWithNewThread()
	{
		AttributesCacheRefreshService service = create();
		service.start();
		AttributesCacheRefreshThread first = service.refreshThread;
		service.stop();

		service.start();

		assertThat(service.refreshThread).isNotSameAs(first);
		assertThat(service.refreshThread.isAlive()).isTrue();
		service.stop();
	}
}
