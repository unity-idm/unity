/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.PendingAttributesCacheEntry;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AttributesCacheRefreshThreadTest
{
	@Mock
	private BulkGroupQueryService bulkGroupQueryService;
	@Mock
	private AttributesCacheDAO attributesCacheDAO;
	@Mock
	private AttributesCachePendingDAO attributesCachePendingDAO;
	@Mock
	private TransactionalRunner tx;

	private java.util.concurrent.ExecutorService executorService;
	private AttributesCacheRefreshThread processor;
	private final List<AttributesCacheRefreshThread> startedThreads = new ArrayList<>();

	@BeforeEach
	public void setup()
	{
		executorService = mock(java.util.concurrent.ExecutorService.class);
		when(executorService.submit(any(Runnable.class))).thenAnswer(inv ->
		{
			((Runnable) inv.getArgument(0)).run();
			return CompletableFuture.completedFuture(null);
		});

		doAnswer(inv ->
		{
			((TransactionalRunner.TxRunnable) inv.getArgument(0)).run();
			return null;
		}).when(tx).runInTransaction(any());

		when(tx.runInTransactionRet(any())).thenAnswer(inv ->
				((TransactionalRunner.TxRunnableRet<?>) inv.getArgument(0)).run());

		processor = new AttributesCacheRefreshThread(bulkGroupQueryService, attributesCacheDAO,
				attributesCachePendingDAO, tx, executorService);
	}

	@AfterEach
	public void stopStartedThreads() throws InterruptedException
	{
		for (AttributesCacheRefreshThread thread : startedThreads)
		{
			thread.requestStop();
			thread.join(2000);
			assertThat(thread.isAlive()).isFalse();
		}
	}

	private AttributesCacheRefreshThread startThread()
	{
		AttributesCacheRefreshThread thread = new AttributesCacheRefreshThread(bulkGroupQueryService,
				attributesCacheDAO, attributesCachePendingDAO, tx, executorService);
		startedThreads.add(thread);
		thread.start();
		return thread;
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<List<StoredAttribute>> storedAttributesCaptor()
	{
		return ArgumentCaptor.forClass(List.class);
	}

	@Test
	public void shouldStoreEffectiveAttributesAndClearPendingFlag() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(new PendingAttributesCacheEntry(1L, "/test", 1L)));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		AttributeExt attr = new AttributeExt(StringAttribute.of("attr", "/test", "value"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data))
				.thenReturn(Map.of(1L, Map.of("attr", attr)));

		processor.refreshAllPending();

		verify(attributesCacheDAO).deleteCacheInGroup(1L, "/test");
		ArgumentCaptor<List<StoredAttribute>> captor = storedAttributesCaptor();
		verify(attributesCacheDAO).createList(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).getAttribute().getName()).isEqualTo("attr");
		assertThat(captor.getValue().get(0).getEntityId()).isEqualTo(1L);
		verify(attributesCachePendingDAO).clearPendingIfVersion(1L, "/test", 1L);
	}

	@Test
	public void shouldClearPendingFlagOnlyInVersionReadBeforeComputation() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(new PendingAttributesCacheEntry(1L, "/test", 7L)));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of(1L, Map.of()));
		// simulates an invalidation committed during the computation: the flag has a newer version now
		when(attributesCachePendingDAO.clearPendingIfVersion(1L, "/test", 7L)).thenReturn(false);

		processor.refreshAllPending();

		verify(attributesCachePendingDAO).clearPendingIfVersion(1L, "/test", 7L);
	}

	@Test
	public void shouldOverrideGroupPathOfGlobalAttributeClone() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(new PendingAttributesCacheEntry(1L, "/test", 1L)));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		// simulates EffectiveAttributesEvaluator.addGlobal(): a clone of a root-group ("/") attribute,
		// which does not get its groupPath rewritten to the group being queried
		AttributeExt globalClone = new AttributeExt(StringAttribute.of("globalAttr", "/", "value"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data))
				.thenReturn(Map.of(1L, Map.of("globalAttr", globalClone)));

		processor.refreshAllPending();

		ArgumentCaptor<List<StoredAttribute>> captor = storedAttributesCaptor();
		verify(attributesCacheDAO).createList(captor.capture());
		assertThat(captor.getValue().get(0).getAttribute().getGroupPath()).isEqualTo("/test");
	}

	@Test
	public void shouldClearCacheAndPendingWhenEntityNoLongerGroupMember() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(new PendingAttributesCacheEntry(1L, "/test", 1L)));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		// entity no longer a member of the group -> absent from the bulk result
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of());

		processor.refreshAllPending();

		verify(attributesCacheDAO).deleteCacheInGroup(1L, "/test");
		verify(attributesCacheDAO).createList(List.of());
		verify(attributesCachePendingDAO).clearPendingIfVersion(1L, "/test", 1L);
	}

	@Test
	public void shouldIsolateFailureOfOneGroupFromOthers() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(
				new PendingAttributesCacheEntry(1L, "/broken", 1L),
				new PendingAttributesCacheEntry(2L, "/ok", 1L)));
		when(bulkGroupQueryService.getBulkMembershipData("/broken"))
				.thenThrow(new RuntimeException("simulated failure"));
		GroupMembershipData okData = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/ok")).thenReturn(okData);
		AttributeExt attr = new AttributeExt(StringAttribute.of("attr", "/ok", "v"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/ok", okData))
				.thenReturn(Map.of(2L, Map.of("attr", attr)));

		processor.refreshAllPending();

		verify(attributesCachePendingDAO, never()).clearPendingIfVersion(eq(1L), anyString(), anyLong());
		verify(attributesCachePendingDAO).clearPendingIfVersion(2L, "/ok", 1L);
	}

	@Test
	public void shouldIsolateFailureOfOneEntityFromOthersInSameGroup() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(
				new PendingAttributesCacheEntry(1L, "/test", 1L),
				new PendingAttributesCacheEntry(2L, "/test", 1L)));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of());
		doThrow(new RuntimeException("simulated store failure"))
				.when(attributesCacheDAO).deleteCacheInGroup(1L, "/test");

		processor.refreshAllPending();

		verify(attributesCachePendingDAO, never()).clearPendingIfVersion(1L, "/test", 1L);
		verify(attributesCachePendingDAO).clearPendingIfVersion(2L, "/test", 1L);
	}

	@Test
	public void wakeUpArrivingDuringProcessingIsNotLost() throws Exception
	{
		CountDownLatch firstCycleStarted = new CountDownLatch(1);
		CountDownLatch releaseFirstCycle = new CountDownLatch(1);
		AtomicInteger cycleCount = new AtomicInteger(0);
		when(attributesCachePendingDAO.getAll()).thenAnswer(inv ->
		{
			if (cycleCount.getAndIncrement() == 0)
			{
				firstCycleStarted.countDown();
				assertThat(releaseFirstCycle.await(5, TimeUnit.SECONDS)).isTrue();
			}
			return List.of();
		});

		AttributesCacheRefreshThread thread = startThread();

		thread.wakeUp();
		assertThat(firstCycleStarted.await(2, TimeUnit.SECONDS)).isTrue();

		// this second wakeUp() arrives while the thread is still busy inside the first cycle
		// (blocked on releaseFirstCycle) - a naive wait()/notify() would lose it
		thread.wakeUp();
		releaseFirstCycle.countDown();

		// if the wakeup was correctly remembered, a second cycle starts promptly once the first
		// one finishes, without waiting for the 30s fallback INTERVAL
		Awaitility.await().atMost(2, TimeUnit.SECONDS)
				.untilAsserted(() -> assertThat(cycleCount.get()).isGreaterThanOrEqualTo(2));
	}

	@Test
	public void stopRequestFinishesIdleThreadPromptly() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of());
		AttributesCacheRefreshThread thread = startThread();

		thread.requestStop();
		thread.join(2000);

		assertThat(thread.isAlive()).isFalse();
	}

	@Test
	public void interruptFinishesThread() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of());
		AttributesCacheRefreshThread thread = startThread();

		thread.interrupt();
		thread.join(2000);

		assertThat(thread.isAlive()).isFalse();
	}

	@Test
	public void stopRequestedDuringCycleLeavesRemainingEntitiesPending() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(
				new PendingAttributesCacheEntry(1L, "/test", 1L),
				new PendingAttributesCacheEntry(2L, "/test", 1L)));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of());
		doAnswer(inv ->
		{
			processor.requestStop();
			return null;
		}).when(attributesCacheDAO).deleteCacheInGroup(1L, "/test");

		processor.refreshAllPending();

		verify(attributesCachePendingDAO).clearPendingIfVersion(1L, "/test", 1L);
		verify(attributesCacheDAO, never()).deleteCacheInGroup(2L, "/test");
		verify(attributesCachePendingDAO, never()).clearPendingIfVersion(eq(2L), anyString(), anyLong());
	}

	@Test
	public void stoppedThreadDoesNoFurtherDaoWork() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of());
		AttributesCacheRefreshThread thread = startThread();
		thread.wakeUp();
		Awaitility.await().atMost(2, TimeUnit.SECONDS)
				.untilAsserted(() -> verify(attributesCachePendingDAO, atLeastOnce()).getAll());

		thread.requestStop();
		thread.join(2000);
		clearInvocations(attributesCachePendingDAO, attributesCacheDAO, bulkGroupQueryService);
		thread.wakeUp();

		assertThat(thread.isAlive()).isFalse();
		verifyNoInteractions(attributesCachePendingDAO, attributesCacheDAO, bulkGroupQueryService);
	}

	@Test
	public void threadRecoversAfterFailedCycle() throws Exception
	{
		when(attributesCachePendingDAO.getAll())
				.thenThrow(new RuntimeException("simulated DB outage"))
				.thenReturn(List.of(new PendingAttributesCacheEntry(1L, "/test", 1L)));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of());
		AttributesCacheRefreshThread thread = startThread();

		thread.wakeUp();
		Awaitility.await().atMost(2, TimeUnit.SECONDS)
				.untilAsserted(() -> verify(attributesCachePendingDAO, atLeastOnce()).getAll());
		thread.wakeUp();

		Awaitility.await().atMost(2, TimeUnit.SECONDS)
				.untilAsserted(() -> verify(attributesCachePendingDAO).clearPendingIfVersion(1L, "/test", 1L));
		assertThat(thread.isAlive()).isTrue();
	}
}
