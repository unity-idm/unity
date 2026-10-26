/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
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
import pl.edu.icm.unity.store.api.AttributesCacheDAO;

/**
 * Scope of the refresh work, measured as the number of bulk loads and of entities whose effective attributes
 * get computed, rather than by timing.
 */
public class AttributesCacheRefreshScopeTest extends AttributesCacheIntegrationTestBase
{
	private static final int LARGE_GROUP_SIZE = 40;

	@Autowired
	@Qualifier("insecure")
	private BulkGroupQueryService bulkGroupQueryService;

	private final List<String> bulkLoadedGroups = new ArrayList<>();
	private final List<Integer> computedEntitiesCounts = new ArrayList<>();

	@Test
	public void startupBacklogIsProcessedWithOneBulkComputationPerGroup() throws Exception
	{
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		for (int i = 0; i < 10; i++)
			addEntity("e" + i, i % 2 == 0 ? "/A" : "/B");
		tx.runInTransaction(() -> attributesCachePendingDAO.markPendingForAll());

		measuringRefresh().refreshAllPending();

		assertThat(pendingPairs()).isEmpty();
		assertThat(bulkLoadedGroups).containsExactlyInAnyOrder("/", "/A", "/B");
	}

	@Test
	public void singleDirtyMemberOfLargeGroupIsTheOnlyOneStored() throws Exception
	{
		long dirty = prepareLargeGroupWithSingleDirtyMember();
		List<Long> storedEntities = new ArrayList<>();

		refreshRecordingStores(storedEntities).refreshAllPending();

		assertThat(storedEntities).containsOnly(dirty);
	}

	@Test
	@Disabled("Known limitation: the refresh computes effective attributes of all members of a group, even when"
			+ " a single one is pending - see the bulk load in AttributesCacheRefreshThread.refreshGroup")
	public void singleDirtyMemberOfLargeGroupDoesNotTriggerComputationForWholeGroup() throws Exception
	{
		prepareLargeGroupWithSingleDirtyMember();

		measuringRefresh().refreshAllPending();

		assertThat(computedEntitiesCounts).allMatch(count -> count == 1);
	}

	private long prepareLargeGroupWithSingleDirtyMember() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/large"));
		List<Long> members = new ArrayList<>();
		for (int i = 0; i < LARGE_GROUP_SIZE; i++)
			members.add(addEntity("m" + i, "/large"));
		measuringRefresh().refreshAllPending();
		assertThat(pendingPairs()).isEmpty();
		bulkLoadedGroups.clear();
		computedEntitiesCounts.clear();

		long dirty = members.get(0);
		attrsMan.createAttribute(new EntityParam(dirty), StringAttribute.of("a1", "/large", "changed"));
		return dirty;
	}

	private AttributesCacheRefreshThread measuringRefresh() throws Exception
	{
		BulkGroupQueryService measuring = mock(BulkGroupQueryService.class, delegatesTo(bulkGroupQueryService));
		doAnswer(inv ->
		{
			bulkLoadedGroups.add(inv.getArgument(0));
			return bulkGroupQueryService.getBulkMembershipData(inv.<String>getArgument(0));
		}).when(measuring).getBulkMembershipData(any());
		doAnswer(inv ->
		{
			Map<Long, Map<String, AttributeExt>> computed = bulkGroupQueryService.getGroupUsersAttributes(
					inv.getArgument(0), inv.<GroupMembershipData>getArgument(1));
			computedEntitiesCounts.add(computed.size());
			return computed;
		}).when(measuring).getGroupUsersAttributes(any(), any());
		return new AttributesCacheRefreshThread(measuring, attributesCacheDAO, attributesCachePendingDAO, tx,
				MoreExecutors.newDirectExecutorService());
	}

	private AttributesCacheRefreshThread refreshRecordingStores(List<Long> storedEntities)
	{
		AttributesCacheDAO recording = mock(AttributesCacheDAO.class, delegatesTo(attributesCacheDAO));
		doAnswer(inv ->
		{
			storedEntities.add(inv.<Long>getArgument(0));
			attributesCacheDAO.deleteCacheInGroup(inv.getArgument(0), inv.getArgument(1));
			return null;
		}).when(recording).deleteCacheInGroup(anyLong(), any());
		return new AttributesCacheRefreshThread(bulkGroupQueryService, recording, attributesCachePendingDAO, tx,
				MoreExecutors.newDirectExecutorService());
	}
}
