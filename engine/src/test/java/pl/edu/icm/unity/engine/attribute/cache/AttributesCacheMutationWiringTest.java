/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.types.EntityInGroup;

/**
 * Mutations through the public engine APIs: which (entity, group) pairs get marked as pending, that the marking
 * is part of the mutation's transaction, and that reads are consistent before and after the refresh.
 */
public class AttributesCacheMutationWiringTest extends AttributesCacheIntegrationTestBase
{
	@Autowired
	private MembershipDAO membershipDAO;

	private long entity;
	private long other;

	@BeforeEach
	public void setup() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		entity = addEntity("e1", "/A");
		other = addEntity("e2", "/A");
		clearAllPending();
	}

	@Test
	public void attributeChangeInvalidatesAllGroupsOfThatEntityOnly() throws Exception
	{
		attrsMan.createAttribute(new EntityParam(entity), StringAttribute.of("a1", "/A", "v1"));

		assertThat(pendingPairs()).containsExactlyInAnyOrder(pair(entity, "/"), pair(entity, "/A"));
	}

	@Test
	public void attributeRemovalInvalidatesAllGroupsOfThatEntityOnly() throws Exception
	{
		attrsMan.createAttribute(new EntityParam(entity), StringAttribute.of("a1", "/A", "v1"));
		clearAllPending();

		attrsMan.removeAttribute(new EntityParam(entity), "/A", "a1");

		assertThat(pendingPairs()).containsExactlyInAnyOrder(pair(entity, "/"), pair(entity, "/A"));
	}

	@Test
	public void newEntityIsPendingInRootGroup() throws Exception
	{
		long created = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "new"), EntityState.valid)
				.getEntityId();

		assertThat(pendingPairs()).containsExactly(pair(created, "/"));
	}

	@Test
	public void identityAdditionInvalidatesThatEntity() throws Exception
	{
		idsMan.addIdentity(new IdentityParam(IdentifierIdentity.ID, "e1-second"), new EntityParam(entity));

		assertThat(pendingPairs()).containsExactlyInAnyOrder(pair(entity, "/"), pair(entity, "/A"));
	}

	@Test
	public void identityRemovalInvalidatesThatEntity() throws Exception
	{
		idsMan.addIdentity(new IdentityParam(IdentifierIdentity.ID, "e1-second"), new EntityParam(entity));
		clearAllPending();

		idsMan.removeIdentity(new IdentityTaV(IdentifierIdentity.ID, "e1-second"));

		assertThat(pendingPairs()).containsExactlyInAnyOrder(pair(entity, "/"), pair(entity, "/A"));
	}

	@Test
	public void entityStatusChangeInvalidatesThatEntity() throws Exception
	{
		idsMan.setEntityStatus(new EntityParam(entity), EntityState.disabled);

		assertThat(pendingPairs()).containsExactlyInAnyOrder(pair(entity, "/"), pair(entity, "/A"));
	}

	@Test
	public void membershipAdditionInvalidatesAllGroupsOfThatEntity() throws Exception
	{
		groupsMan.addMemberFromParent("/B", new EntityParam(entity));

		assertThat(pendingPairs()).containsExactlyInAnyOrder(
				pair(entity, "/"), pair(entity, "/A"), pair(entity, "/B"));
	}

	@Test
	public void membershipRemovalInvalidatesRemainingGroupsOfThatEntity() throws Exception
	{
		groupsMan.removeMember("/A", new EntityParam(entity));

		assertThat(pendingPairs()).contains(pair(entity, "/"));
		assertThat(pendingPairs()).allMatch(p -> p.entityId() == entity);
	}

	@Test
	public void refreshOfDepartedMemberClearsItsCacheAndPendingFlag() throws Exception
	{
		attrsMan.createAttribute(new EntityParam(entity), StringAttribute.of("a1", "/A", "v1"));
		startBackgroundRefresh();
		awaitFreshCachedValues(entity, "/A", "a1", List.of("v1"));
		refreshService.stop();

		groupsMan.removeMember("/A", new EntityParam(entity));
		assertThat(isPending(entity, "/A")).isTrue();
		startBackgroundRefresh();

		// nothing to store for a non member: the refresh stores an empty list, which must not fail
		org.awaitility.Awaitility.await().pollInSameThread().atMost(15, java.util.concurrent.TimeUnit.SECONDS)
				.untilAsserted(() -> assertThat(isPending(entity, "/A")).isFalse());
		assertThat(tx.runInTransactionRet(() -> attributesCacheDAO.getEntityAttributes(entity, "/A"))).isEmpty();
	}

	@Test
	public void entityAttributeClassesChangeInvalidatesThatEntity() throws Exception
	{
		acMan.addAttributeClass(new AttributesClass("ac1", "", Set.of("a1"), Set.of(), false, Set.of()));
		clearAllPending();

		acMan.setEntityAttributeClasses(new EntityParam(entity), "/A", List.of("ac1"));

		assertThat(pendingPairs()).containsExactlyInAnyOrder(pair(entity, "/"), pair(entity, "/A"));
	}

	@Test
	public void attributeTypeChangesInvalidateEverything() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a2", StringAttributeSyntax.ID));
		assertThat(pendingPairs()).isEqualTo(allMemberships());
		clearAllPending();

		AttributeType updated = new AttributeType("a2", StringAttributeSyntax.ID);
		updated.setMaxElements(5);
		aTypeMan.updateAttributeType(updated);
		assertThat(pendingPairs()).isEqualTo(allMemberships());
		clearAllPending();

		aTypeMan.removeAttributeType("a2", true);
		assertThat(pendingPairs()).isEqualTo(allMemberships());
	}

	@Test
	public void attributeClassChangesInvalidateEverything() throws Exception
	{
		acMan.addAttributeClass(new AttributesClass("ac1", "", Set.of("a1"), Set.of(), false, Set.of()));
		assertThat(pendingPairs()).isEqualTo(allMemberships());
		clearAllPending();

		acMan.removeAttributeClass("ac1");
		assertThat(pendingPairs()).isEqualTo(allMemberships());
	}

	@Test
	public void rolledBackMutationLeavesNoPendingFlag()
	{
		assertThatThrownBy(() -> tx.runInTransactionThrowing(() ->
		{
			attrsMan.createAttribute(new EntityParam(entity), StringAttribute.of("a1", "/A", "v1"));
			throw new IllegalStateException("simulated failure after the mutation");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(pendingPairs()).isEmpty();
	}

	@Test
	public void consistentReadReflectsMutationBeforeRefreshAndCacheConvergesAfter() throws Exception
	{
		attrsMan.createAttribute(new EntityParam(entity), StringAttribute.of("a1", "/A", "v1"));

		assertThat(isPending(entity, "/A")).isTrue();
		assertThat(effectiveAttributesCache.getAttributes(entity, "/A").get("a1").getValues())
				.containsExactly("v1");

		startBackgroundRefresh();

		awaitFreshCachedValues(entity, "/A", "a1", List.of("v1"));
		assertThat(effectiveAttributesCache.getAttributes(entity, "/A").get("a1").getValues())
				.containsExactly("v1");
		awaitFreshCachedValues(other, "/A", "a1", null);
	}

	private Set<EntityInGroup> allMemberships()
	{
		return tx.runInTransactionRet(() -> membershipDAO.getAll().stream()
				.map(m -> pair(m.getEntityId(), m.getGroup()))
				.collect(java.util.stream.Collectors.toSet()));
	}
}
