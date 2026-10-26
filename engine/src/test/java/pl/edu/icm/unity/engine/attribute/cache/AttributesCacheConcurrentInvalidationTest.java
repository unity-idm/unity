/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.util.concurrent.MoreExecutors;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Runs the refresh on a real DB, with a state change committed after the refresh computed the effective
 * attributes, but before it stored them.
 */
public class AttributesCacheConcurrentInvalidationTest extends AttributesCacheIntegrationTestBase
{
	private static final long TIMEOUT_S = 10;

	@Autowired
	@Qualifier("insecure")
	private BulkGroupQueryService bulkGroupQueryService;

	private long entityId;
	private EntityParam entity;

	@BeforeEach
	public void setup() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		entityId = addEntity("e1", "/A");
		entity = new EntityParam(entityId);
		attrsMan.createAttribute(entity, StringAttribute.of("a1", "/A", "v1"));
	}

	@Test
	public void invalidationCommittedDuringRefreshSurvivesAndIsAppliedInNextCycle() throws Exception
	{
		AtomicBoolean mutated = new AtomicBoolean();
		AttributesCacheRefreshThread refresh = refreshWithHookAfterComputation("/A", () ->
		{
			if (mutated.compareAndSet(false, true))
				attrsMan.setAttribute(entity, StringAttribute.of("a1", "/A", "v2"));
		});

		refresh.refreshAllPending();

		assertThat(mutated).isTrue();
		assertThat(isPending(entityId, "/A")).isTrue();

		refresh.refreshAllPending();

		assertThat(isPending(entityId, "/A")).isFalse();
		assertThat(cachedValues("/A")).containsExactly("v2");
	}

	@Test
	public void mutationCommittedByAnotherThreadDuringRefreshCannotDisappear() throws Exception
	{
		CountDownLatch computed = new CountDownLatch(1);
		CountDownLatch mutationCommitted = new CountDownLatch(1);
		AtomicBoolean first = new AtomicBoolean(true);
		AttributesCacheRefreshThread refresh = refreshWithHookAfterComputation("/A", () ->
		{
			if (!first.getAndSet(false))
				return;
			computed.countDown();
			assertThat(mutationCommitted.await(TIMEOUT_S, TimeUnit.SECONDS)).isTrue();
		});
		CompletableFuture<Void> mutation = CompletableFuture.runAsync(() ->
		{
			awaitLatch(computed);
			setAttributeInsecure("v2");
			mutationCommitted.countDown();
		});

		refresh.refreshAllPending();
		mutation.get(TIMEOUT_S, TimeUnit.SECONDS);

		assertThat(isPending(entityId, "/A")).isTrue();
		refresh.refreshAllPending();
		assertThat(isPending(entityId, "/A")).isFalse();
		assertThat(cachedValues("/A")).containsExactly("v2");
	}

	@Test
	public void mutationInOpenTransactionDuringPublicationCannotDisappear() throws Exception
	{
		CountDownLatch computed = new CountDownLatch(1);
		CountDownLatch mutationMarked = new CountDownLatch(1);
		AtomicBoolean first = new AtomicBoolean(true);
		AttributesCacheRefreshThread refresh = refreshWithHookAfterComputation("/A", () ->
		{
			if (!first.getAndSet(false))
				return;
			computed.countDown();
			assertThat(mutationMarked.await(TIMEOUT_S, TimeUnit.SECONDS)).isTrue();
		});
		CompletableFuture<Void> mutation = CompletableFuture.runAsync(() ->
		{
			awaitLatch(computed);
			tx.runInTransaction(() ->
			{
				setAttributeInsecure("v2");
				mutationMarked.countDown();
				// keeps the pending flag row locked while the refresh tries to publish and clear the flag
				sleep(300);
			});
		});

		refresh.refreshAllPending();
		mutation.get(TIMEOUT_S, TimeUnit.SECONDS);

		assertThat(isPending(entityId, "/A")).isTrue();
		refresh.refreshAllPending();
		assertThat(isPending(entityId, "/A")).isFalse();
		assertThat(cachedValues("/A")).containsExactly("v2");
	}

	private AttributesCacheRefreshThread refreshWithHookAfterComputation(String hookedGroup, ThrowingRunnable hook)
	{
		BulkGroupQueryService bulkWithHook = mock(BulkGroupQueryService.class, delegatesTo(bulkGroupQueryService));
		doAnswer(inv ->
		{
			String group = inv.getArgument(0);
			Map<Long, Map<String, AttributeExt>> computed = bulkGroupQueryService.getGroupUsersAttributes(group,
					inv.<GroupMembershipData>getArgument(1));
			if (hookedGroup.equals(group))
				hook.run();
			return computed;
		}).when(bulkWithHook).getGroupUsersAttributes(any(), any());
		return new AttributesCacheRefreshThread(bulkWithHook, attributesCacheDAO, attributesCachePendingDAO, tx,
				MoreExecutors.newDirectExecutorService());
	}

	private void setAttributeInsecure(String value)
	{
		try
		{
			insecureAttrsMan.setAttribute(entity, StringAttribute.of("a1", "/A", value));
		} catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	private List<String> cachedValues(String group)
	{
		return tx.runInTransactionRet(() -> attributesCacheDAO.getEntityAttributes(entityId, group)).stream()
				.map(StoredAttribute::getAttribute)
				.filter(a -> a.getName().equals("a1"))
				.findFirst()
				.orElseThrow()
				.getValues();
	}

	private static void awaitLatch(CountDownLatch latch)
	{
		try
		{
			assertThat(latch.await(TIMEOUT_S, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static void sleep(long ms)
	{
		try
		{
			Thread.sleep(ms);
		} catch (InterruptedException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable
	{
		void run() throws Exception;
	}
}
