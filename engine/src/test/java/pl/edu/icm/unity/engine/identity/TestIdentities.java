/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.api.identity.UnknownEmailException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;


public class TestIdentities extends DBIntegrationTestBase
{
	@Autowired
	private EntitiesScheduledUpdater entitiesUpdater;
	
	@BeforeEach
	public void prepare() throws Exception
	{
		setupMockAuthn();	
	}
	
	@Test
	public void shouldUpdateIdentityConfirmation() throws Exception
	{
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "test@example.com");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);

		//verify
		Entity retrieved = idsMan.getEntity(new EntityParam(idParam));
		assertThat(getByType(retrieved, EmailIdentity.ID).getConfirmationInfo().isConfirmed()).isFalse();
		
		Identity updated = id.clone();
		updated.setConfirmationInfo(new ConfirmationInfo(true));
		idsMan.updateIdentity(idParam, updated);
		
		retrieved = idsMan.getEntity(new EntityParam(idParam));
		assertThat(getByType(retrieved, EmailIdentity.ID).getConfirmationInfo().isConfirmed()).isTrue();
	}

	@Test
	public void shouldDisallowUpdatingIdentityComparableValue() throws Exception
	{
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "test@example.com");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		Identity updated = id.clone();
		updated.setValue("other@example.com");
		
		Throwable error = catchThrowable(() -> idsMan.updateIdentity(idParam, updated));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void scheduledDisableWork() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		EntityParam ep1 = new EntityParam(id.getEntityId());
		Date scheduledTime = new Date(System.currentTimeMillis()+100);
		idsMan.scheduleEntityChange(ep1, scheduledTime, EntityScheduledOperation.DISABLE);
		
		Entity retrieved = idsMan.getEntity(new EntityParam(idParam));
		assertEquals(EntityScheduledOperation.DISABLE,
				retrieved.getEntityInformation().getScheduledOperation());
		assertEquals(scheduledTime,
				retrieved.getEntityInformation().getScheduledOperationTime());
		
		Thread.sleep(150);
		entitiesUpdater.updateEntities();
		
		Entity updated = idsMan.getEntity(ep1);
		assertEquals(EntityState.disabled, updated.getState());
	}

	@Test
	public void scheduledRemovalWork() throws Exception
	{
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2");
		Identity id2 = idsMan.addEntity(idParam2, "crMock", EntityState.valid);
		
		EntityParam ep2 = new EntityParam(id2.getEntityId());
		Date scheduledTime = new Date(System.currentTimeMillis()+100);
		idsMan.scheduleEntityChange(ep2, scheduledTime, EntityScheduledOperation.REMOVE);
		
		Entity retrieved = idsMan.getEntity(ep2);
		assertEquals(EntityScheduledOperation.REMOVE,
				retrieved.getEntityInformation().getScheduledOperation());
		assertEquals(scheduledTime,
				retrieved.getEntityInformation().getScheduledOperationTime());

		Thread.sleep(150);
		entitiesUpdater.updateEntities();
		
		try
		{
			idsMan.getEntity(ep2);
			fail("Entity not removed");
		} catch (IllegalArgumentException e)
		{
			//ok
		}
	}

	@Test
	public void typesForAllSyntaxesAreReturned() throws Exception
	{
		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();
		assertEquals(7, idTypes.size());
		assertNotNull(getIdentityTypeByName(idTypes, PersistentIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, TargetedPersistentIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, X500Identity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, UsernameIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, TransientIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, IdentifierIdentity.ID));
	}

	@Test
	public void updatedTypeIsReturned() throws Exception
	{
		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();
		aTypeMan.addAttributeType(new AttributeType("cn", StringAttributeSyntax.ID));
		AttributeType at = new AttributeType("country", StringAttributeSyntax.ID);
		at.setMaxElements(0);
		aTypeMan.addAttributeType(at);

		IdentityType toUpdate = getIdentityTypeByName(idTypes, X500Identity.ID);
		toUpdate.setDescription("fiu fiu");
		idTypeMan.updateIdentityType(toUpdate);
		
		idTypes = idTypeMan.getIdentityTypes();
		IdentityType updated = getIdentityTypeByName(idTypes, X500Identity.ID);
		assertThat(updated).isEqualTo(toUpdate);
	}
	
	@Test
	public void onlyPersistentAddedWhenAllowedWithoutTarget() throws Exception
	{
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		idsMan.resetIdentity(entityParam, PersistentIdentity.ID, null, null);
		
		Entity e2 = idsMan.getEntity(entityParam, null, true, "/");
		
		assertEquals(2, e2.getIdentities().size());
		assertNotNull(getByType(e2, X500Identity.ID));
		assertTrue(getByType(e2, PersistentIdentity.ID).getValue().length() > 0);
	}

	@Test
	public void shouldCreatePersistentIdentityWithEntity() throws Exception
	{
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		
		Entity e2 = idsMan.getEntity(entityParam, null, false, "/");
		
		assertEquals(2, e2.getIdentities().size());
		assertNotNull(getByType(e2, X500Identity.ID));
		assertTrue(getByType(e2, PersistentIdentity.ID).getValue().length() > 0);
	}

	
	@Test
	public void allAddedWhenAllowedWithTarget() throws Exception
	{
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		
		Entity e3 = idsMan.getEntity(entityParam, "target1", true, "/");

		assertEquals(4, e3.getIdentities().size());
		assertNotNull(getByType(e3, X500Identity.ID));
		assertTrue(getByType(e3, PersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TargetedPersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TransientIdentity.ID).getValue().length() > 0);
	}
	
	@Test
	public void differentTargetedIdentitiesAreCreatedForDifferentTargets() throws Exception
	{
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		
		Entity e3 = idsMan.getEntity(entityParam, "target1", true, "/");
		assertEquals(4, e3.getIdentities().size());
		assertNotNull(getByType(e3, X500Identity.ID));
		assertTrue(getByType(e3, PersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TargetedPersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TransientIdentity.ID).getValue().length() > 0);
		
		Entity e4 = idsMan.getEntity(entityParam, "target2", true, "/");
		assertEquals(4, e4.getIdentities().size());
		assertNotNull(getByType(e4, X500Identity.ID));
		assertTrue(getByType(e4, PersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e4, TargetedPersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e4, TransientIdentity.ID).getValue().length() > 0);

		assertNotEquals(getByType(e3, TransientIdentity.ID).getValue(), 
				getByType(e4, TransientIdentity.ID).getValue());
		assertNotEquals(getByType(e3, TargetedPersistentIdentity.ID).getValue(), 
				getByType(e4, TargetedPersistentIdentity.ID).getValue());
		assertEquals(getByType(e3, PersistentIdentity.ID).getValue(), 
				getByType(e4, PersistentIdentity.ID).getValue());
		Entity e2 = idsMan.getEntity(entityParam, null, true, "/");
		assertEquals(getByType(e2, PersistentIdentity.ID).getValue(), 
				getByType(e4, PersistentIdentity.ID).getValue());
	}	
	
	@Test
	public void getEntityTriggersCreationOfPersistentIdentity() throws Exception
	{
		setupAdmin();

		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2");
		Identity id2 = idsMan.addEntity(idParam2, "crMock", EntityState.valid);
		EntityParam entityParam2 = new EntityParam(id2.getEntityId());

		Entity e5 = idsMan.getEntity(entityParam2);
		assertEquals(2, e5.getIdentities().size());
		assertNotNull(getByType(e5, X500Identity.ID));
		assertTrue(getByType(e5, PersistentIdentity.ID).getValue().length() > 0);
	}

	@Test
	public void identitiesWithSameTypeAndDifferentTypeAreDistinguished() throws Exception
	{
		IdentityParam idParam = new IdentityParam(UsernameIdentity.ID, "id");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		IdentityParam idParam2 = new IdentityParam(IdentifierIdentity.ID, "id");
		idsMan.addIdentity(idParam2, new EntityParam(id.getEntityId()));
		
		Entity ret = idsMan.getEntity(new EntityParam(id.getEntityId()), null, false, "/");
		
		assertThat(ret.getIdentities()).hasSize(3);
		assertThat(getIdentityByType(ret.getIdentities(), UsernameIdentity.ID).getValue()).isEqualTo("id");
		assertThat(getIdentityByType(ret.getIdentities(), IdentifierIdentity.ID).getValue()).isEqualTo("id");
	}

	@Test
	public void removingLastIdentityIsProhibited() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		idsMan.resetIdentity(new EntityParam(id.getEntityId()), PersistentIdentity.ID, null, null);
		
		Throwable error = catchThrowable(() -> idsMan.removeIdentity(id));

		assertThat(error).isInstanceOf(SchemaConsistencyException.class);
	}
	
	
	@Test
	public void entityAddedToTopGroupIsReturned() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		groupsMan.addGroup(new Group("/test"));
		groupsMan.addMemberFromParent("/test", new EntityParam(id.getEntityId()));
		
		GroupContents contents = groupsMan.getContents("/test", GroupContents.MEMBERS);
		
		assertEquals(1, contents.getMembers().size());
		assertEquals(id.getEntityId(), contents.getMembers().get(0).getEntityId());
	}
	
	@Test
	public void addedEntityIsReturend() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		Entity entity = idsMan.getEntity(new EntityParam(id));

		assertNotNull(id.getEntityId());
		assertEquals("CN=golbi", id.getValue());
		assertEquals(true, id.isLocal());
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertNull(id.getTranslationProfile());
		assertNull(id.getRemoteIdp());
		
		assertEquals(2, entity.getIdentities().size());
		assertEquals(id, getByName(entity, X500Identity.ID, "CN=golbi"));
		getByType(entity, PersistentIdentity.ID);
		assertEquals(id.getEntityId(), entity.getId().longValue());
	}
	
	@Test
	public void identityAddedToEntityIsReturned() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2", "remoteIdp", "prof1");
		Identity id2 = idsMan.addIdentity(idParam2, new EntityParam(id.getEntityId()));

		Entity entity = idsMan.getEntity(new EntityParam(id2));
		
		assertEquals("CN=golbi2", id2.getValue());
		assertEquals(id.getEntityId(), id2.getEntityId());
		assertEquals(false, id2.isLocal());
		assertNotNull(id2.getCreationTs());
		assertNotNull(id2.getUpdateTs());
		assertEquals("prof1", id2.getTranslationProfile());
		assertEquals("remoteIdp", id2.getRemoteIdp());
		
		assertEquals(3, entity.getIdentities().size());
		assertEquals(id, getByName(entity, X500Identity.ID, "CN=golbi"));
		getByType(entity, PersistentIdentity.ID);
		Identity retDn = getByName(entity, X500Identity.ID, "CN=golbi2");
		assertEquals(id2, retDn);
		assertEquals(id.getEntityId(), entity.getId().longValue());
	}
	
	@Test
	public void removedIdentityIsNotReturned() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2", "remoteIdp", "prof1");
		Identity id2 = idsMan.addIdentity(idParam2, new EntityParam(id.getEntityId()));
		
		idsMan.removeIdentity(id);
		
		Entity entity = idsMan.getEntity(new EntityParam(id2));
		assertEquals(2, entity.getIdentities().size());
		Identity retdnp = getByName(entity, X500Identity.ID, "CN=golbi2");
		assertEquals(id2, retdnp);
		assertEquals(id2.getEntityId(), entity.getId().longValue());
	}
	
	@Test
	public void removedEntityIsNotReturned() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		idsMan.removeEntity(new EntityParam(id));

		Throwable e = catchThrowable(() -> idsMan.getEntity(new EntityParam(id)));
		
		assertThat(e).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void removedEntityIsRemovedFromRootGroup() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		idsMan.removeEntity(new EntityParam(id));

		GroupContents contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		assertEquals(1, contents.getMembers().size());
	}
	
	@Test
	public void disabledStatusIsReturned() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		idsMan.setEntityStatus(new EntityParam(id), EntityState.disabled);
	
		Entity entity = idsMan.getEntity(new EntityParam(id));	
		assertEquals(EntityState.disabled, entity.getState());
	}
	
	
	@Test
	public void shouldFailToAddToSubgoupWhenNotInParent() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		groupsMan.addGroup(new Group("/test2"));
		groupsMan.addGroup(new Group("/test2/test"));
		
		Throwable error = catchThrowable(() -> groupsMan.addMemberFromParent("/test2/test", new EntityParam(id.getEntityId())));
		
		assertThat(error).isInstanceOf(IllegalGroupValueException.class);
	}
	
	@Test
	public void shouldNotRemoveFromRootGroup() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		Throwable error = catchThrowable(() -> groupsMan.removeMember("/", new EntityParam(id.getEntityId())));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldFailRemovalFromGroupWithoutMembership() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		groupsMan.addGroup(new Group("/test2"));

		Throwable error = catchThrowable(() -> groupsMan.removeMember("/test2", new EntityParam(id.getEntityId())));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void addedEntityBecomesRootMember() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		GroupContents contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		assertEquals(2, contents.getMembers().size());
		assertTrue(contents.getMembers().stream().anyMatch(t -> id.getEntityId() == t.getEntityId()));
	}
	
	
	@Test
	public void entityRemovedFromParentGroupIsRemovedFromItAndSubgroup() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid);
		
		groupsMan.addGroup(new Group("/test2"));
		groupsMan.addGroup(new Group("/test2/test"));
		groupsMan.addMemberFromParent("/test2", new EntityParam(id.getEntityId()));
		groupsMan.addMemberFromParent("/test2/test", new EntityParam(id.getEntityId()));
		
		groupsMan.removeMember("/test2", new EntityParam(id.getEntityId()));
		
		GroupContents t2Contents = groupsMan.getContents("/test2", GroupContents.MEMBERS);
		assertEquals(0, t2Contents.getMembers().size());
		GroupContents contents = groupsMan.getContents("/test2/test", GroupContents.MEMBERS);
		assertEquals(0, contents.getMembers().size());
	}
	
	@Test
	public void shouldNotRetEntitiesWithContactEmailWhenAtChanged() throws EngineException
	{
		AttributeType at = new AttributeType();
		at.setDescription(new I18nString("desc"));
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName("at");
		at.setSelfModificable(true);
		at.setValueSyntax(VerifiableEmailAttributeSyntax.ID);
		at.setMetadata(Map.of("contactEmail", ""));
		aTypeMan.addAttributeType(at);
		
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "username99"), 
				CR_MOCK, EntityState.valid);
		attrsMan.createAttribute(new EntityParam(added1), VerifiableEmailAttribute.of("at", "/", new VerifiableEmail("demo@demo.com")));
		
		Set<EntityWithContactInfo> allEntitiesWithContactEmails = idsMan.getAllEntitiesWithContactEmails(Set.of("demo@demo.com"));
		assertThat(allEntitiesWithContactEmails.size()).isEqualTo(1);
		assertThat(allEntitiesWithContactEmails.iterator().next().entity.getId()).isEqualTo(added1.getEntityId());
		
		at.setMetadata(new HashMap<>());
		aTypeMan.updateAttributeType(at);
		
		Throwable error = catchThrowable(() -> idsMan.getAllEntitiesWithContactEmails(Set.of("demo@demo.com")));
		assertThat(error).isInstanceOf(UnknownEmailException.class);
	}
	
	@Test
	public void shouldReturnIdentitiesForExistingEntities() throws EngineException
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "username01"),
				CR_MOCK, EntityState.valid);
		Identity added2 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "username02"),
				CR_MOCK, EntityState.valid);
		idsMan.addIdentity(new IdentityParam(UsernameIdentity.ID, "username03"), new EntityParam(added2));

		Entity entity1 = idsMan.getEntity(new EntityParam(added1));
		Entity entity2 = idsMan.getEntity(new EntityParam(added2));

		Map<Long, List<Identity>> identitiesForEntities = idsMan.getIdentitiesForEntities(
				Set.of(added1.getEntityId(), added2.getEntityId()));

		assertThat(identitiesForEntities).containsOnlyKeys(added1.getEntityId(), added2.getEntityId());
		assertThat(identitiesForEntities.get(added1.getEntityId()))
				.containsExactlyInAnyOrderElementsOf(entity1.getIdentities());
		assertThat(identitiesForEntities.get(added2.getEntityId()))
				.containsExactlyInAnyOrderElementsOf(entity2.getIdentities());
	}

	@Test
	public void shouldIgnoreReturningIdentitiesForMissingEntities() throws EngineException
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "username01"),
				CR_MOCK, EntityState.valid);
		Entity entity1 = idsMan.getEntity(new EntityParam(added1));

		Map<Long, List<Identity>> identitiesForEntities = idsMan.getIdentitiesForEntities(
				Set.of(added1.getEntityId(), 999999L));

		assertThat(identitiesForEntities).containsOnlyKeys(added1.getEntityId());
		assertThat(identitiesForEntities.get(added1.getEntityId()))
				.containsExactlyInAnyOrderElementsOf(entity1.getIdentities());
	}

	@Test
	public void shouldReturnIdentitiesForExistingEntitiesBulk() throws EngineException
	{
		List<Entity> added = new ArrayList<>();
		for (int i=10; i<EntityManagementImpl.ITERATIVE_IDENTITIES_RESOLVING_THRESHOLD + 11; i++)
		{
			Identity id = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "username" + i),
					CR_MOCK, EntityState.valid);
			added.add(idsMan.getEntity(new EntityParam(id)));
		}


		Map<Long, List<Identity>> identitiesForEntities = idsMan.getIdentitiesForEntities(
				added.stream().map(Entity::getId).collect(Collectors.toSet()));

		assertThat(identitiesForEntities).hasSize(added.size());
		for (Entity entity : added)
			assertThat(identitiesForEntities.get(entity.getId()))
				.containsExactlyInAnyOrderElementsOf(entity.getIdentities());
	}

	@Test
	public void shouldIgnoreReturningIdentitiesForMissingEntitiesBulk() throws EngineException
	{
		Map<Long, List<Identity>> identitiesForEntities = idsMan.getIdentitiesForEntities(
				LongStream.range(10, EntityManagementImpl.ITERATIVE_IDENTITIES_RESOLVING_THRESHOLD + 11)
						.boxed()
						.collect(Collectors.toSet()));

		assertThat(identitiesForEntities).isEmpty();
	}

	private Identity getByType(Entity e, String type)
	{
		for (Identity id: e.getIdentities())
			if (id.getTypeId().equals(type))
				return id;
		fail("No such type");
		return null;
	}

	private Identity getByName(Entity e, String type, String name)
	{
		for (Identity id: e.getIdentities())
			if (id.getTypeId().equals(type) && id.getValue().equals(name))
				return id;
		fail("No such type");
		return null;
	}
}
