/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.internal.EntitiesScheduledUpdater;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;

public class TestIdentities extends DBIntegrationTestBase
{
	@Autowired
	private EntitiesScheduledUpdater entitiesUpdater;
	
	@Test
	public void scheduledOpsWork() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2");
		Identity id2 = idsMan.addEntity(idParam2, "crMock", EntityState.valid, false);
		
		EntityParam ep1 = new EntityParam(id.getEntityId());
		Date scheduledTime = new Date(System.currentTimeMillis()+100);
		idsMan.scheduleEntityChange(ep1, scheduledTime, EntityScheduledOperation.DISABLE);
		
		Entity retrieved = idsMan.getEntity(new EntityParam(idParam));
		assertEquals(EntityScheduledOperation.DISABLE,
				retrieved.getEntityInformation().getScheduledOperation());
		assertEquals(scheduledTime,
				retrieved.getEntityInformation().getScheduledOperationTime());
		
		EntityParam ep2 = new EntityParam(id2.getEntityId());
		idsMan.scheduleEntityChange(ep2, new Date(System.currentTimeMillis()+100), 
				EntityScheduledOperation.REMOVE);
		Thread.sleep(200);
		entitiesUpdater.updateEntities();
		
		Entity updated = idsMan.getEntity(ep1);
		assertEquals(EntityState.disabled, updated.getState());
		
		try
		{
			idsMan.getEntity(ep2);
			fail("Entity not removed");
		} catch (IllegalIdentityValueException e)
		{
			//ok
		}
	}

	@Test
	public void scheduledRemovalWorksForUser() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());
		
		setupUserContext("user1", false);
		
		idsMan.scheduleRemovalByUser(ep1, new Date(System.currentTimeMillis()+200));
		idsMan.getEntity(ep1);
		Thread.sleep(200);
		entitiesUpdater.updateEntities();
		
		try
		{
			idsMan.getEntity(ep1);
			fail("Entity not removed");
		} catch (IllegalIdentityValueException e)
		{
			//ok
		}
	}

	@Test
	public void scheduledRemovalWorksForUserImmediately() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());
		
		setupUserContext("user1", false);
		
		idsMan.scheduleRemovalByUser(ep1, new Date(System.currentTimeMillis()));
		try
		{
			idsMan.getEntity(ep1);
			fail("Entity not removed");
		} catch (IllegalIdentityValueException e)
		{
			//ok
		}
	}

	@Test
	public void scheduledRemovalGraceTimeWorksForUser() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());

		setupUserContext("user1", false);
		idsMan.scheduleRemovalByUser(ep1, new Date(System.currentTimeMillis()+500));
		setupUserContext("user1", false);
		entitiesUpdater.updateEntities();
		
		Entity entity = idsMan.getEntity(ep1);
		assertEquals(EntityState.valid, entity.getState());
	}
	
	@Test
	public void testSyntaxes() throws Exception
	{
		List<IdentityType> idTypes = idsMan.getIdentityTypes();
		assertEquals(7, idTypes.size());
		assertNotNull(getIdentityTypeByName(idTypes, PersistentIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, TargetedPersistentIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, X500Identity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, UsernameIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, TransientIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, IdentifierIdentity.ID));
		
		IdentityType toUpdate = getIdentityTypeByName(idTypes, X500Identity.ID);
		toUpdate.setDescription("fiu fiu");
		Map<String, String> extracted = new HashMap<String, String>();
		extracted.put("cn", "cn");
		toUpdate.setExtractedAttributes(extracted);
		try
		{
			idsMan.updateIdentityType(toUpdate);
			fail("managed to set attributes extraction with undefined attribute t");
		} catch(IllegalAttributeTypeException e) {}
		attrsMan.addAttributeType(new AttributeType("cn", new StringAttributeSyntax()));
		extracted.put("unknown", "cn");
		toUpdate.setExtractedAttributes(extracted);
		try
		{
			idsMan.updateIdentityType(toUpdate);
			fail("managed to set attributes extraction with unsupported attribute t");
		} catch(IllegalAttributeTypeException e) {}
		extracted.remove("unknown");
		
		AttributeType at = new AttributeType("country", new StringAttributeSyntax());
		at.setMaxElements(0);
		attrsMan.addAttributeType(at);
		extracted.put("c", "country");
		toUpdate.setExtractedAttributes(extracted);
		idsMan.updateIdentityType(toUpdate);
		
		
		idTypes = idsMan.getIdentityTypes();
		IdentityType updated = getIdentityTypeByName(idTypes, X500Identity.ID);
		assertEquals("fiu fiu", updated.getDescription());
		assertEquals(2, updated.getExtractedAttributes().size());
		assertEquals("cn", updated.getExtractedAttributes().keySet().iterator().next());
		assertEquals("cn", updated.getExtractedAttributes().values().iterator().next());
		
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi, dc=ddd, ou=org unit,C=pl");
		Identity added = idsMan.addEntity(idParam, "crMock", EntityState.valid, true);
		
		Collection<AttributeExt<?>> attributes = attrsMan.getAttributes(new EntityParam(added), "/", null);
		assertEquals(1, attributes.size());
		Attribute<?> cnAttr = getAttributeByName(attributes, "cn");
		assertEquals(cnAttr.getValues().get(0), "golbi");
		
		attrsMan.removeAttributeType("cn", true);
		idTypes = idsMan.getIdentityTypes();
		updated = getIdentityTypeByName(idTypes, X500Identity.ID);
		assertEquals(1, updated.getExtractedAttributes().size());
		assertEquals("c", updated.getExtractedAttributes().keySet().iterator().next());
		assertEquals("country", updated.getExtractedAttributes().values().iterator().next());
		
		attrsMan.updateAttributeType(new AttributeType("country", new IntegerAttributeSyntax()));
		idTypes = idsMan.getIdentityTypes();
		updated = getIdentityTypeByName(idTypes, X500Identity.ID);
		assertEquals(0, updated.getExtractedAttributes().size());
	}

	@Test
	public void testDynamic() throws Exception
	{
		setupMockAuthn();
		setupAdmin();

		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		assertNotNull(id.getEntityId());
		assertEquals("CN=golbi", id.getValue());
		assertEquals(true, id.isLocal());
		
		EntityParam entityParam = new EntityParam(id.getEntityId());
		
		Entity e1 = idsMan.getEntity(entityParam, null, false, "/");
		assertEquals(1, e1.getIdentities().length);
		assertEquals(X500Identity.ID, e1.getIdentities()[0].getTypeId());
		
		Entity e2 = idsMan.getEntity(entityParam, null, true, "/");
		assertEquals(2, e2.getIdentities().length);
		assertNotNull(getByType(e2, X500Identity.ID));
		assertTrue(getByType(e2, PersistentIdentity.ID).getValue().length() > 0);
		
		Entity e3 = idsMan.getEntity(entityParam, "target1", true, "/");
		assertEquals(4, e3.getIdentities().length);
		assertNotNull(getByType(e3, X500Identity.ID));
		assertTrue(getByType(e3, PersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TargetedPersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TransientIdentity.ID).getValue().length() > 0);
		
		Entity e4 = idsMan.getEntity(entityParam, "target2", true, "/");
		assertEquals(4, e4.getIdentities().length);
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
		assertEquals(getByType(e2, PersistentIdentity.ID).getValue(), 
				getByType(e4, PersistentIdentity.ID).getValue());
		
		
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2");
		Identity id2 = idsMan.addEntity(idParam2, "crMock", EntityState.valid, false);
		EntityParam entityParam2 = new EntityParam(id2.getEntityId());

		Entity e5 = idsMan.getEntity(entityParam2);
		assertEquals(2, e5.getIdentities().length);
		assertNotNull(getByType(e5, X500Identity.ID));
		assertTrue(getByType(e5, PersistentIdentity.ID).getValue().length() > 0);
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
	
	@Test
	public void testCreate() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		assertNotNull(id.getEntityId());
		assertEquals("CN=golbi", id.getValue());
		assertEquals(true, id.isLocal());
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertNull(id.getTranslationProfile());
		assertNull(id.getRemoteIdp());
		
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2", "remoteIdp", "prof1");
		Identity id2 = idsMan.addIdentity(idParam2, new EntityParam(id.getEntityId()), false);
		assertEquals("CN=golbi2", id2.getValue());
		assertEquals(id.getEntityId(), id2.getEntityId());
		assertEquals(false, id2.isLocal());
		assertNotNull(id2.getCreationTs());
		assertNotNull(id2.getUpdateTs());
		assertEquals("prof1", id2.getTranslationProfile());
		assertEquals("remoteIdp", id2.getRemoteIdp());
		
		Entity entity = idsMan.getEntity(new EntityParam(id2));
		assertEquals(3, entity.getIdentities().length);
		assertEquals(id, getByName(entity, X500Identity.ID, "CN=golbi"));
		getByType(entity, PersistentIdentity.ID);
		Identity retDn = getByName(entity, X500Identity.ID, "CN=golbi2");
		assertEquals(id2, retDn);
		assertEquals(id.getEntityId(), entity.getId());
		
		idsMan.setEntityStatus(new EntityParam(entity.getId()), EntityState.disabled);
		entity = idsMan.getEntity(new EntityParam(id2));
		assertEquals(EntityState.disabled, entity.getState());
		
		GroupContents contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		assertEquals(2, contents.getMembers().size());
		assertTrue(contents.getMembers().contains(id.getEntityId()));

		groupsMan.addGroup(new Group("/test"));
		groupsMan.addMemberFromParent("/test", new EntityParam(id.getEntityId()));
		contents = groupsMan.getContents("/test", GroupContents.MEMBERS);
		assertEquals(1, contents.getMembers().size());
		assertEquals(id.getEntityId(), contents.getMembers().get(0));
		
		
		groupsMan.addGroup(new Group("/test2"));
		groupsMan.addGroup(new Group("/test2/test"));
		try
		{
			groupsMan.addMemberFromParent("/test2/test", new EntityParam(id.getEntityId()));
			fail("Added to a group while is not in parent");
		} catch(IllegalGroupValueException e) {}
		

		try
		{
			groupsMan.removeMember("/", new EntityParam(id.getEntityId()));
			fail("removed member from /");
		} catch(IllegalGroupValueException e) {}

		try
		{
			groupsMan.removeMember("/test2", new EntityParam(id.getEntityId()));
			fail("removed non member");
		} catch(IllegalGroupValueException e) {}

		groupsMan.addMemberFromParent("/test2", new EntityParam(id.getEntityId()));
		groupsMan.addMemberFromParent("/test2/test", new EntityParam(id.getEntityId()));
		groupsMan.removeMember("/test2", new EntityParam(id.getEntityId()));
		GroupContents t2Contents = groupsMan.getContents("/test2/test", GroupContents.MEMBERS);
		assertEquals(0, t2Contents.getMembers().size());
		
		groupsMan.removeMember("/test", new EntityParam(id.getEntityId()));
		contents = groupsMan.getContents("/test", GroupContents.MEMBERS);
		assertEquals(0, contents.getMembers().size());
		
		idsMan.removeIdentity(id);
		entity = idsMan.getEntity(new EntityParam(id2));
		assertEquals(2, entity.getIdentities().length);
		Identity retdnp = getByName(entity, X500Identity.ID, "CN=golbi2");
		assertEquals(id2, retdnp);
		assertEquals(id2.getEntityId(), entity.getId());
		
		idsMan.removeEntity(new EntityParam(id2));
		
		try
		{
			idsMan.getEntity(new EntityParam(id2));
			fail("Removed entity is still available");
		} catch (IllegalIdentityValueException e) {}
		
		contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		assertEquals(1, contents.getMembers().size());
	}
}
