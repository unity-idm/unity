/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class AttributesManagementImplTest extends DBIntegrationTestBase
{
	private EntityParam entity;
	
	@Before
	public void setup() throws Exception
	{
		setupMockAuthn();
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), "crMock", 
				EntityState.valid, false);
		entity = new EntityParam(id.getEntityId());
		groupsMan.addGroup(new Group("/test"));
		aTypeMan.addAttributeType(new AttributeType("tel", StringAttributeSyntax.ID));
	}	
	
	@Test
	public void systemAttributeCantBeRemoved() throws Exception
	{
		Attribute systemA = StringAttribute.of(
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS, "/", 
				"asdsadsa"); 
		try
		{
			attrsMan.setAttribute(entity, systemA);
			fail("Updated immutable attribute");
		} catch (SchemaConsistencyException e) {}
	}	
	
	@Test
	public void systemAttributeCantBeUpdated() throws Exception
	{
		Attribute systemA = StringAttribute.of(
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS, "/", 
				"asdsadsa"); 
		try
		{
			attrsMan.removeAttribute(entity, "/", systemA.getName());
			fail("Removed immutable attribute");
		} catch (SchemaConsistencyException e) {}
	}	

	@Test
	public void cantSetAttributeInGroupWhereEntityIsntMember() throws Exception
	{
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		try
		{
			attrsMan.createAttribute(entity, at1);
			fail("Added attribute in a group where entity is not a member");
		} catch (IllegalGroupValueException e) {}
	}

	@Test
	public void addedAttributesAreReturned() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		attrsMan.createAttribute(entity, at1);
		
		Attribute at2 = StringAttribute.of("tel", "/", "1234");
		attrsMan.createAttribute(entity, at2);
		
		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(DEF_ATTRS + 2, allAts.size());
		Collection<AttributeExt> gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(DEF_ATTRS + 1, gr1Ats.size());
		AttributeExt retrievedA = getAttributeByName(gr1Ats, "tel"); 
		assertEquals(at2.getValues(), retrievedA.getValues());
		assertNotNull(retrievedA.getUpdateTs());
		assertNotNull(retrievedA.getCreationTs());
		assertNull(retrievedA.getRemoteIdp());
		assertNull(retrievedA.getTranslationProfile());
		Collection<AttributeExt> nameAts = attrsMan.getAttributes(entity, null, "tel");
		assertEquals(2, nameAts.size());
		Collection<AttributeExt> specificAts = attrsMan.getAttributes(entity, "/test", "tel");
		assertEquals(1, specificAts.size());
		assertEquals(at1.getValues(), specificAts.iterator().next().getValues());
	}

	
	@Test
	public void removedAttributeIsNotReturned() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		attrsMan.createAttribute(entity, at1);
		Attribute at2 = StringAttribute.of("tel", "/", "1234");
		attrsMan.createAttribute(entity, at2);
		
		attrsMan.removeAttribute(entity, "/", "tel");
		Collection<AttributeExt> gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(DEF_ATTRS, gr1Ats.size());
		Collection<AttributeExt> gr2Ats = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(1, gr2Ats.size());
		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(DEF_ATTRS + 1, allAts.size());
	}

	@Test
	public void afterRemovalFromGroupAttributesCantBeGet() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		attrsMan.createAttribute(entity, at1);
		
		groupsMan.removeMember("/test", entity);
		try
		{
			attrsMan.getAttributes(entity, "/test", null);
			fail("no error when asking for attributes in group where the user is not a member");
		} catch (IllegalGroupValueException e) {}
	}

	@Test
	public void shouldNotSetSystemManagerRoleHoldingContentsManager() throws Exception
	{
		Attribute managerRoleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.CONTENTS_MANAGER_ROLE);
		EntityParam adminEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		insecureAttrsMan.setAttribute(adminEntity, managerRoleAt);
		
		setupUserContext("admin", null);
		
		Attribute systemManagerRoleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE);
		
		
		catchException(attrsMan).setAttribute(adminEntity, systemManagerRoleAt);
		
		assertThat(caughtException(), isA(AuthorizationException.class));
	}

	@Test
	public void shouldSetInspectorRoleHoldingContentsManager() throws Exception
	{
		Attribute managerRoleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.CONTENTS_MANAGER_ROLE);
		EntityParam adminEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		insecureAttrsMan.setAttribute(adminEntity, managerRoleAt);
		
		setupUserContext("admin", null);
		
		Attribute inspectorRoleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.PRIVILEGED_INSPECTOR_ROLE);
		
		
		attrsMan.setAttribute(adminEntity, inspectorRoleAt);
	}

	@Test
	public void shouldSetUserRoleHoldingSystemManagerROle() throws Exception
	{
		Attribute managerRoleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE);
		EntityParam adminEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		insecureAttrsMan.setAttribute(adminEntity, managerRoleAt);
		
		setupUserContext("admin", null);
		
		Attribute inspectorRoleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.USER_ROLE);
		
		
		attrsMan.setAttribute(adminEntity, inspectorRoleAt);
	}
	
	@Test
	public void afterRemovalFromGroupAttributesAreRemoved() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		attrsMan.createAttribute(entity, at1);
		
		groupsMan.removeMember("/test", entity);

		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(allAts.toString(), DEF_ATTRS, allAts.size());
	}

	
	@Test
	public void afterRemovalAndReaddToGroupOriginalAttributesAreRemoved() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		attrsMan.createAttribute(entity, at1);
		
		groupsMan.removeMember("/test", entity);
		groupsMan.addMemberFromParent("/test", entity);

		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(allAts.toString(), 0, allAts.size());
	}
	
	@Test
	public void afterCreateTimestampsAreSame() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		attrsMan.createAttribute(entity, at1);
		
		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(1, allAts.size());
		
		AttributeExt retrievedA = allAts.iterator().next();
		Date created = retrievedA.getCreationTs(); 
		Date updated = retrievedA.getUpdateTs(); 
		assertEquals(created, updated);
	}

	@Test
	public void afterUpdateTimestampsIsChanged() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		Attribute at1 = StringAttribute.of("tel", "/test", "123456");
		attrsMan.createAttribute(entity, at1);
		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(1, allAts.size());
		AttributeExt retrievedA = allAts.iterator().next();
		Date created = retrievedA.getCreationTs();
		Date updated = retrievedA.getUpdateTs(); 
		
		at1.setValues(Collections.singletonList("333"));
		Thread.sleep(2);
		attrsMan.setAttribute(entity, at1);
		
		allAts = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(1, allAts.size());
		retrievedA = allAts.iterator().next();
		assertEquals(created, retrievedA.getCreationTs());
		assertNotEquals(updated, retrievedA.getUpdateTs());
		assertNotNull(retrievedA.getUpdateTs());
	}

	@Test
	public void existingAttributeSetIsForbiddenWithoutUpdateFlag() throws Exception
	{
		Attribute at2 = StringAttribute.of("tel", "/", "1234");
		attrsMan.createAttribute(entity, at2);
		try
		{
			attrsMan.createAttribute(entity, at2);
			fail("updated existing attribute without update flag");
		} catch (IllegalAttributeValueException e) {}
	}

	@Test
	public void updatedAttributeIsReturned() throws Exception
	{
		Attribute at2 = StringAttribute.of("tel", "/", "1234");
		attrsMan.createAttribute(entity, at2);
		
		at2.setValues(Collections.singletonList("333"));
		attrsMan.setAttribute(entity, at2);
		
		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, "/", "tel");
		assertEquals(1, allAts.size());
		assertEquals("333", getAttributeByName(allAts, "tel").getValues().get(0));

		allAts = attrsMan.getAllAttributes(entity, false, "/", "tel", false);
		assertEquals(1, allAts.size());
		assertEquals("333", getAttributeByName(allAts, "tel").getValues().get(0));
		
		allAts = attrsMan.getAllAttributes(entity, true, null, null, false);
		assertEquals(DEF_ATTRS + 1, allAts.size());
		assertEquals("333", getAttributeByName(allAts, "tel").getValues().get(0));
	}

	@Test
	public void thereAreOnlySystemInitialAttributes() throws Exception
	{
		groupsMan.addMemberFromParent("/test", entity);
		
		Collection<AttributeExt> gr2Ats = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(0, gr2Ats.size());
		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(DEF_ATTRS, allAts.size());
	}
	
	@Test
	public void credentialsAreNotReturnedWithCallToGetAttributes() throws Exception
	{
		eCredMan.setEntityCredential(entity, "credential1", new PasswordToken("password").toJson());
		
		Collection<AttributeExt> rootAttrs = attrsMan.getAttributes(entity, "/", null);
		
		assertEquals(DEF_ATTRS, rootAttrs.size());
	}
	
	@Test
	public void credentialsAreReturnedWithCallToGetAllAttributes() throws Exception
	{
		eCredMan.setEntityCredential(entity, "credential1", new PasswordToken("password").toJson());
		
		Collection<AttributeExt> rootAttrs = attrsMan.getAllAttributes(entity, true, "/", null, false);
		
		assertEquals(DEF_ATTRS + 1, rootAttrs.size());
	}
}


