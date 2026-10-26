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

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeMismatch;
import pl.edu.icm.unity.engine.api.attributes.EntityAttributeMismatches;
import pl.edu.icm.unity.engine.api.attributes.GroupValidationReport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
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

	private StoredAttribute cached(long entityId, String group, String name, String... values)
	{
		return new StoredAttribute(new AttributeExt(StringAttribute.of(name, group, values), true), entityId);
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
		assertThat(mismatch.mismatches().get(0).valuesRedacted()).isFalse();
	}

	@Test
	public void mismatchOfCredentialAttributeIsReportedWithoutValues() throws Exception
	{
		String credAttr = setupCredentialMismatch();

		AttributeCacheValidationReport report = service.validate(Optional.of("/"));

		var mismatch = report.groups().get(0).entitiesWithMismatches().get(0).mismatches().get(0);
		assertThat(mismatch.attributeName()).isEqualTo(credAttr);
		assertThat(mismatch.valuesRedacted()).isTrue();
		assertThat(mismatch.cachedValues()).isEmpty();
		assertThat(mismatch.freshValues()).isEmpty();
	}

	@Test
	public void credentialValuesOfMismatchAreNotLoggedEvenAtTrace() throws Exception
	{
		String credAttr = setupCredentialMismatch();

		List<String> logged;
		try (LogCapture capture = LogCapture.capture(
				Log.getLogger(Log.U_SERVER_CORE, AttributeCacheValidationServiceImpl.class), Level.TRACE))
		{
			service.validate(Optional.of("/"));
			logged = capture.getMessages();
		}

		assertThat(logged).anyMatch(m -> m.contains("mismatch") && m.contains(credAttr));
		assertThat(logged).noneMatch(m -> m.contains("old-secret-hash") || m.contains("new-secret-hash"));
	}

	private String setupCredentialMismatch() throws Exception
	{
		create();
		String credAttr = CredentialAttributeTypeProvider.CREDENTIAL_PREFIX + "password";
		when(groupDAO.exists("/")).thenReturn(true);
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/")).thenReturn(data);
		AttributeExt fresh = new AttributeExt(StringAttribute.of(credAttr, "/", "new-secret-hash"), true);
		when(bulkGroupQueryService.getGroupUsersAttributes("/", data))
				.thenReturn(Map.of(1L, Map.of(credAttr, fresh)));
		when(attributesCachePendingDAO.getPendingForGroup("/")).thenReturn(List.of());
		when(attributesCacheDAO.getGroupAttributes("/"))
				.thenReturn(List.of(cached(1L, "/", credAttr, "old-secret-hash")));
		return credAttr;
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

	@Test
	public void attributeMissingInCacheIsReported() throws Exception
	{
		AttributeMismatch mismatch = singleMismatch(
				Map.of("attr", fresh("attr", "v")),
				List.of());

		assertThat(mismatch).isEqualTo(new AttributeMismatch("attr", List.of(), List.of("v"), false));
	}

	@Test
	public void obsoleteCachedAttributeIsReported() throws Exception
	{
		AttributeMismatch mismatch = singleMismatch(
				Map.of(),
				List.of(cached(1L, "/test", "attr", "obsolete")));

		assertThat(mismatch).isEqualTo(new AttributeMismatch("attr", List.of("obsolete"), List.of(), false));
	}

	@Test
	public void changeOfOneOfMultipleValuesIsReported() throws Exception
	{
		AttributeMismatch mismatch = singleMismatch(
				Map.of("attr", fresh("attr", "a", "c")),
				List.of(cached(1L, "/test", "attr", "a", "b")));

		assertThat(mismatch).isEqualTo(new AttributeMismatch("attr", List.of("a", "b"), List.of("a", "c"), false));
	}

	@Test
	public void mixOfPendingFreshAndMismatchingEntitiesIsFullyReported() throws Exception
	{
		create();
		when(groupDAO.exists("/test")).thenReturn(true);
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of(
				1L, Map.of("attr", fresh("attr", "new")),
				2L, Map.of("attr", fresh("attr", "same")),
				3L, Map.of("attr", fresh("attr", "new"), "other", fresh("other", "x"))));
		when(attributesCachePendingDAO.getPendingForGroup("/test"))
				.thenReturn(List.of(new EntityInGroup(1L, "/test")));
		when(attributesCacheDAO.getGroupAttributes("/test")).thenReturn(List.of(
				cached(1L, "/test", "attr", "stale"),
				cached(2L, "/test", "attr", "same"),
				cached(3L, "/test", "attr", "stale"),
				cached(3L, "/test", "other", "x")));

		AttributeCacheValidationReport report = service.validate(Optional.of("/test"));

		assertThat(report).isEqualTo(new AttributeCacheValidationReport(List.of(
				new GroupValidationReport("/test", List.of(1L), List.of(
						new EntityAttributeMismatches(3L, List.of(
								new AttributeMismatch("attr", List.of("stale"), List.of("new"), false))))))));
	}

	private AttributeMismatch singleMismatch(Map<String, AttributeExt> freshAttributes, List<StoredAttribute> cache)
			throws Exception
	{
		create();
		when(groupDAO.exists("/test")).thenReturn(true);
		GroupMembershipData data = mock(GroupMembershipData.class);
		when(bulkGroupQueryService.getBulkMembershipData("/test")).thenReturn(data);
		when(bulkGroupQueryService.getGroupUsersAttributes("/test", data)).thenReturn(Map.of(1L, freshAttributes));
		when(attributesCachePendingDAO.getPendingForGroup("/test")).thenReturn(List.of());
		when(attributesCacheDAO.getGroupAttributes("/test")).thenReturn(cache);

		AttributeCacheValidationReport report = service.validate(Optional.of("/test"));

		List<EntityAttributeMismatches> entities = report.groups().get(0).entitiesWithMismatches();
		assertThat(entities).hasSize(1);
		assertThat(entities.get(0).entityId()).isEqualTo(1L);
		assertThat(entities.get(0).mismatches()).hasSize(1);
		return entities.get(0).mismatches().get(0);
	}

	private static AttributeExt fresh(String name, String... values)
	{
		return new AttributeExt(StringAttribute.of(name, "/test", values), true);
	}

	@Test
	public void deniedAuthorizationPreventsDataAccessAndComputation() throws Exception
	{
		create();
		org.mockito.Mockito.doThrow(new pl.edu.icm.unity.engine.api.authn.AuthorizationException("denied"))
				.when(authz).checkAuthorization(AuthzCapability.maintenance);

		assertThatThrownBy(() -> service.validate(Optional.empty()))
				.isInstanceOf(pl.edu.icm.unity.engine.api.authn.AuthorizationException.class);
		org.mockito.Mockito.verifyNoInteractions(groupDAO, bulkGroupQueryService, attributesCacheDAO,
				attributesCachePendingDAO);
	}
}
