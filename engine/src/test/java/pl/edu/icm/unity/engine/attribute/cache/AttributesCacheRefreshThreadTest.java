/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
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
import pl.edu.icm.unity.store.types.EntityInGroup;
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

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<List<StoredAttribute>> storedAttributesCaptor()
	{
		return ArgumentCaptor.forClass(List.class);
	}

	@Test
	public void shouldStoreEffectiveAttributesAndClearPendingFlag() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(new EntityInGroup(1L, "/test")));
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
		verify(attributesCachePendingDAO).clearPending(1L, "/test");
	}

	@Test
	public void shouldOverrideGroupPathOfGlobalAttributeClone() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(new EntityInGroup(1L, "/test")));
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
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(new EntityInGroup(1L, "/test")));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		// entity no longer a member of the group -> absent from the bulk result
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of());

		processor.refreshAllPending();

		verify(attributesCacheDAO).deleteCacheInGroup(1L, "/test");
		verify(attributesCacheDAO).createList(List.of());
		verify(attributesCachePendingDAO).clearPending(1L, "/test");
	}

	@Test
	public void shouldIsolateFailureOfOneGroupFromOthers() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(
				new EntityInGroup(1L, "/broken"),
				new EntityInGroup(2L, "/ok")));
		when(bulkGroupQueryService.getBulkMembershipData("/broken"))
				.thenThrow(new RuntimeException("simulated failure"));
		GroupMembershipData okData = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/ok")).thenReturn(okData);
		AttributeExt attr = new AttributeExt(StringAttribute.of("attr", "/ok", "v"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/ok", okData))
				.thenReturn(Map.of(2L, Map.of("attr", attr)));

		processor.refreshAllPending();

		verify(attributesCachePendingDAO, never()).clearPending(eq(1L), anyString());
		verify(attributesCachePendingDAO).clearPending(2L, "/ok");
	}

	@Test
	public void shouldIsolateFailureOfOneEntityFromOthersInSameGroup() throws Exception
	{
		when(attributesCachePendingDAO.getAll()).thenReturn(List.of(
				new EntityInGroup(1L, "/test"),
				new EntityInGroup(2L, "/test")));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of());
		doThrow(new RuntimeException("simulated store failure"))
				.when(attributesCacheDAO).deleteCacheInGroup(1L, "/test");

		processor.refreshAllPending();

		verify(attributesCachePendingDAO, never()).clearPending(1L, "/test");
		verify(attributesCachePendingDAO).clearPending(2L, "/test");
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

		AttributesCacheRefreshThread thread = new AttributesCacheRefreshThread(bulkGroupQueryService,
				attributesCacheDAO, attributesCachePendingDAO, tx, executorService);
		thread.start();

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
}
