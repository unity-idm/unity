/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.GroupValidationReport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.types.EntityInGroup;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(MockitoExtension.class)
public class AttributeCacheValidationServiceImplTest
{
	@Mock
	private InternalAuthorizationManager authz;
	@Mock
	private GroupDAO groupDAO;
	@Mock
	private BulkGroupQueryService bulkGroupQueryService;
	@Mock
	private AttributesCacheDAO attributesCacheDAO;
	@Mock
	private AttributesCachePendingDAO attributesCachePendingDAO;

	private AttributeCacheValidationServiceImpl service;

	private void create()
	{
		service = new AttributeCacheValidationServiceImpl(authz, groupDAO, bulkGroupQueryService,
				attributesCacheDAO, attributesCachePendingDAO);
	}

	private StoredAttribute cached(long entityId, String group, String name, String value)
	{
		return new StoredAttribute(new AttributeExt(StringAttribute.of(name, group, value), true), entityId);
	}

	@Test
	public void checksMaintenanceAuthorization() throws Exception
	{
		create();
		when(groupDAO.exists("/test")).thenReturn(true);
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of());
		when(attributesCachePendingDAO.getPendingForGroup("/test")).thenReturn(List.of());
		when(attributesCacheDAO.getGroupAttributes("/test")).thenReturn(List.of());

		service.validate(Optional.of("/test"));

		verify(authz).checkAuthorization(AuthzCapability.maintenance);
	}

	@Test
	public void failsForNonExistingGroup()
	{
		create();
		when(groupDAO.exists("/missing")).thenReturn(false);

		assertThatThrownBy(() -> service.validate(Optional.of("/missing")))
				.isInstanceOf(GroupNotFoundException.class);
	}

	@Test
	public void validatesAllGroupsWhenNoneGiven() throws Exception
	{
		create();
		when(groupDAO.getAllNames()).thenReturn(Set.of("/a", "/b"));
		for (String g : List.of("/a", "/b"))
		{
			GroupMembershipData data = mock(GroupMembershipData.class);
			when(bulkGroupQueryService.getBulkMembershipData(g)).thenReturn(data);
			when(bulkGroupQueryService.getGroupUsersAttributes(g, data)).thenReturn(Map.of());
			when(attributesCachePendingDAO.getPendingForGroup(g)).thenReturn(List.of());
			when(attributesCacheDAO.getGroupAttributes(g)).thenReturn(List.of());
		}

		AttributeCacheValidationReport report = service.validate(Optional.empty());

		assertThat(report.groups()).extracting(GroupValidationReport::group)
				.containsExactlyInAnyOrder("/a", "/b");
	}

	@Test
	public void pendingEntityIsListedAndExcludedFromComparison() throws Exception
	{
		create();
		when(groupDAO.exists("/test")).thenReturn(true);
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		AttributeExt fresh = new AttributeExt(StringAttribute.of("attr", "/test", "fresh-value"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data))
				.thenReturn(Map.of(1L, Map.of("attr", fresh)));
		when(attributesCachePendingDAO.getPendingForGroup("/test"))
				.thenReturn(List.of(new EntityInGroup(1L, "/test")));
		// deliberately stale/absent cache content for entity 1 - must be ignored since it is pending
		when(attributesCacheDAO.getGroupAttributes("/test")).thenReturn(List.of());

		AttributeCacheValidationReport report = service.validate(Optional.of("/test"));

		GroupValidationReport groupReport = report.groups().get(0);
		assertThat(groupReport.entitiesWithPendingUpdate()).containsExactly(1L);
		assertThat(groupReport.entitiesWithMismatches()).isEmpty();
	}

	@Test
	public void mismatchIsReportedWithCachedAndFreshValues() throws Exception
	{
		create();
		when(groupDAO.exists("/test")).thenReturn(true);
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		AttributeExt fresh = new AttributeExt(StringAttribute.of("attr", "/test", "correct"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data))
				.thenReturn(Map.of(1L, Map.of("attr", fresh)));
		when(attributesCachePendingDAO.getPendingForGroup("/test")).thenReturn(List.of());
		when(attributesCacheDAO.getGroupAttributes("/test"))
				.thenReturn(List.of(cached(1L, "/test", "attr", "stale")));

		AttributeCacheValidationReport report = service.validate(Optional.of("/test"));

		GroupValidationReport groupReport = report.groups().get(0);
		assertThat(groupReport.entitiesWithPendingUpdate()).isEmpty();
		assertThat(groupReport.entitiesWithMismatches()).hasSize(1);
		var mismatch = groupReport.entitiesWithMismatches().get(0);
		assertThat(mismatch.entityId()).isEqualTo(1L);
		assertThat(mismatch.mismatches()).hasSize(1);
		assertThat(mismatch.mismatches().get(0).attributeName()).isEqualTo("attr");
		assertThat(mismatch.mismatches().get(0).cachedValues()).containsExactly("stale");
		assertThat(mismatch.mismatches().get(0).freshValues()).containsExactly("correct");
	}

	@Test
	public void noMismatchWhenCacheMatchesFresh() throws Exception
	{
		create();
		when(groupDAO.exists("/test")).thenReturn(true);
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		AttributeExt fresh = new AttributeExt(StringAttribute.of("attr", "/test", "same"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data))
				.thenReturn(Map.of(1L, Map.of("attr", fresh)));
		when(attributesCachePendingDAO.getPendingForGroup("/test")).thenReturn(List.of());
		when(attributesCacheDAO.getGroupAttributes("/test"))
				.thenReturn(List.of(cached(1L, "/test", "attr", "same")));

		AttributeCacheValidationReport report = service.validate(Optional.of("/test"));

		assertThat(report.groups().get(0).entitiesWithMismatches()).isEmpty();
	}
}
