/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * The refresh worker follows the Spring context lifecycle: started on refresh, stopped (and terminated) on
 * close, so that recreating contexts doesn't accumulate workers.
 */
public class AttributesCacheRefreshSpringLifecycleTest
{
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@AfterEach
	public void shutdownExecutor()
	{
		executor.shutdownNow();
	}

	@Test
	public void workerIsStartedWithContextAndTerminatedOnClose()
	{
		long workersBefore = aliveWorkers();

		AnnotationConfigApplicationContext context = createContext();
		AttributesCacheRefreshService service = context.getBean(AttributesCacheRefreshService.class);
		assertThat(service.isRunning()).isTrue();
		assertThat(aliveWorkers()).isEqualTo(workersBefore + 1);
		AttributesCacheRefreshThread worker = service.refreshThread;

		context.close();

		assertThat(service.isRunning()).isFalse();
		assertThat(worker.isAlive()).isFalse();
		assertThat(aliveWorkers()).isEqualTo(workersBefore);
	}

	@Test
	public void recreatingContextDoesNotAccumulateWorkers()
	{
		long workersBefore = aliveWorkers();

		for (int i = 0; i < 3; i++)
		{
			AnnotationConfigApplicationContext context = createContext();
			assertThat(aliveWorkers()).isEqualTo(workersBefore + 1);
			context.close();
		}

		assertThat(aliveWorkers()).isEqualTo(workersBefore);
	}

	private AnnotationConfigApplicationContext createContext()
	{
		TransactionalRunner tx = mock(TransactionalRunner.class);
		when(tx.runInTransactionRet(any())).thenReturn(List.of());
		ExecutorsService executorsService = mock(ExecutorsService.class);
		when(executorsService.getExecutionService()).thenReturn(executor);

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		// bean name is matched by the @Qualifier("insecure") of the service constructor
		context.registerBean("insecure", BulkGroupQueryService.class, () -> mock(BulkGroupQueryService.class));
		context.registerBean(AttributesCacheDAO.class, () -> mock(AttributesCacheDAO.class));
		context.registerBean(AttributesCachePendingDAO.class, () -> mock(AttributesCachePendingDAO.class));
		context.registerBean(TransactionalRunner.class, () -> tx);
		context.registerBean(ExecutorsService.class, () -> executorsService);
		context.registerBean(AttributesCacheRefreshService.class);
		context.refresh();
		return context;
	}

	private static long aliveWorkers()
	{
		return Thread.getAllStackTraces().keySet().stream()
				.filter(t -> t.isAlive() && "AttributesCacheRefresh".equals(t.getName()))
				.count();
	}
}
