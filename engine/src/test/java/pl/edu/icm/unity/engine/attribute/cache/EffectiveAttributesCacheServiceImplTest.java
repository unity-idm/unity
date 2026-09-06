/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.engine.api.attributes.CachedAttributes;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.types.EntityInGroup;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(MockitoExtension.class)
public class EffectiveAttributesCacheServiceImplTest
{
	@Mock
	private AttributesCacheDAO attributesCacheDAO;
	@Mock
	private AttributesCachePendingDAO attributesCachePendingDAO;
	@Mock
	private MembershipDAO membershipDAO;
	@Mock
	private BulkGroupQueryService bulkGroupQueryService;
	@Mock
	private AttributesHelper attributesHelper;

	private EffectiveAttributesCacheServiceImpl service;

	private EffectiveAttributesCacheServiceImpl create()
	{
		return new EffectiveAttributesCacheServiceImpl(attributesCacheDAO, attributesCachePendingDAO,
				membershipDAO, bulkGroupQueryService, attributesHelper);
	}

	private StoredAttribute attribute(long entityId, String group, String name, String value)
	{
		return new StoredAttribute(new AttributeExt(StringAttribute.of(name, group, value), true), entityId);
	}

	@Test
	public void consistentSingleEntityReadsCacheWhenFresh() throws Exception
	{
		service = create();
		when(attributesCachePendingDAO.isPending(1L, "/test")).thenReturn(false);
		when(attributesCacheDAO.getEntityAttributes(1L, "/test"))
				.thenReturn(List.of(attribute(1L, "/test", "attr", "v")));

		Map<String, AttributeExt> result = service.getAttributes(1L, "/test");

		assertThat(result).containsOnlyKeys("attr");
		verify(attributesHelper, never()).getAllAttributesAsMapOneGroup(1L, "/test");
	}

	@Test
	public void consistentSingleEntityComputesLiveWhenPending() throws Exception
	{
		service = create();
		when(attributesCachePendingDAO.isPending(1L, "/test")).thenReturn(true);
		Map<String, AttributeExt> live = Map.of("attr", new AttributeExt(StringAttribute.of("attr", "/test", "fresh"), true));
		when(attributesHelper.getAllAttributesAsMapOneGroup(1L, "/test")).thenReturn(live);

		Map<String, AttributeExt> result = service.getAttributes(1L, "/test");

		assertThat(result).isSameAs(live);
		verify(attributesCacheDAO, never()).getEntityAttributes(1L, "/test");
	}

	@Test
	public void consistentGroupReadsCacheWhenNothingPending() throws Exception
	{
		service = create();
		when(attributesCachePendingDAO.getPendingForGroup("/test")).thenReturn(List.of());
		when(attributesCacheDAO.getGroupAttributes("/test"))
				.thenReturn(List.of(attribute(1L, "/test", "attr", "v")));

		Map<Long, Map<String, AttributeExt>> result = service.getGroupAttributes("/test");

		assertThat(result).containsOnlyKeys(1L);
		assertThat(result.get(1L)).containsOnlyKeys("attr");
		verify(bulkGroupQueryService, never()).getBulkMembershipData(anyString());
	}

	@Test
	public void consistentGroupComputesLiveWhenAnyEntityPending() throws Exception
	{
		service = create();
		when(attributesCachePendingDAO.getPendingForGroup("/test"))
				.thenReturn(List.of(new EntityInGroup(1L, "/test")));
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		Map<Long, Map<String, AttributeExt>> live = Map.of(1L, Map.of("attr",
				new AttributeExt(StringAttribute.of("attr", "/test", "fresh"), true)));
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(live);

		Map<Long, Map<String, AttributeExt>> result = service.getGroupAttributes("/test");

		assertThat(result).isSameAs(live);
		verify(attributesCacheDAO, never()).getGroupAttributes("/test");
	}

	@Test
	public void fastSingleEntityAlwaysReadsCacheAndReportsPendingFlag()
	{
		service = create();
		when(attributesCacheDAO.getEntityAttributes(1L, "/test"))
				.thenReturn(List.of(attribute(1L, "/test", "attr", "v")));
		when(attributesCachePendingDAO.isPending(1L, "/test")).thenReturn(true);

		CachedAttributes result = service.getAttributesFast(1L, "/test");

		assertThat(result.attributes()).containsOnlyKeys("attr");
		assertThat(result.updatePending()).isTrue();
	}

	@Test
	public void fastGroupIncludesAllMembersEvenWithoutCacheOrPending()
	{
		service = create();
		when(attributesCacheDAO.getGroupAttributes("/test"))
				.thenReturn(List.of(attribute(1L, "/test", "attr", "v")));
		when(attributesCachePendingDAO.getPendingForGroup("/test"))
				.thenReturn(List.of(new EntityInGroup(2L, "/test")));
		when(membershipDAO.getMembers("/test")).thenReturn(List.of(
				new GroupMembership("/test", 1L, new Date()),
				new GroupMembership("/test", 2L, new Date()),
				new GroupMembership("/test", 3L, new Date())));

		Map<Long, CachedAttributes> result = service.getGroupAttributesFast("/test");

		assertThat(result).containsOnlyKeys(1L, 2L, 3L);
		assertThat(result.get(1L).attributes()).containsOnlyKeys("attr");
		assertThat(result.get(1L).updatePending()).isFalse();
		assertThat(result.get(2L).attributes()).isEmpty();
		assertThat(result.get(2L).updatePending()).isTrue();
		assertThat(result.get(3L).attributes()).isEmpty();
		assertThat(result.get(3L).updatePending()).isFalse();
	}
}
