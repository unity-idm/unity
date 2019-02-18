/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.exceptions.MergeConflictException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestMerge extends DBIntegrationTestBase
{
	@Autowired
	private AttributeClassManagement acMan;
	@Autowired
	private CredentialRequirementManagement crMan;
	
	@Before
	public void setup() throws Exception
	{
		setupPasswordAuthn(1, false);
	}
	
	@Test
	public void sourceEntityRemoved() throws Exception
	{
		Identity target = createUsernameUser("target", InternalAuthorizationManagerImpl.USER_ROLE, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", InternalAuthorizationManagerImpl.USER_ROLE, "p2", CRED_REQ_PASS);
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), false);
		
		try
		{
			idsMan.getEntity(new EntityParam(merged.getEntityId()));
			fail("Merged entity still valid");
		} catch (IllegalArgumentException e)
		{
			//OK
		}
	}

	@Test
	public void regularIdentitiesAdded() throws Exception
	{
		Identity target = createUsernameUser("target", InternalAuthorizationManagerImpl.USER_ROLE, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", InternalAuthorizationManagerImpl.USER_ROLE, "p2", CRED_REQ_PASS);
		Entity mergedFull = idsMan.getEntity(new EntityParam(merged), "tt", true, "/");
		Entity targetFull = idsMan.getEntity(new EntityParam(target), null, true, "/");
		idsMan.addIdentity(new IdentityParam(IdentifierIdentity.ID, "id"), new EntityParam(merged), false);
		

		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), false);
		
		
		Entity result = idsMan.getEntityNoContext(new EntityParam(target.getEntityId()), "/");
		
		//regular - simply added
		Collection<Identity> idT = getIdentitiesByType(result.getIdentities(), IdentifierIdentity.ID);
		assertEquals(1, idT.size());
		assertEquals("id", idT.iterator().next().getValue());

		//dynamic but added as target has no one of this type
		Collection<Identity> srcT = getIdentitiesByType(mergedFull.getIdentities(), 
				TargetedPersistentIdentity.ID);
		assertEquals(1, srcT.size());
		idT = getIdentitiesByType(result.getIdentities(), TargetedPersistentIdentity.ID);
		assertEquals(1, idT.size());
		assertEquals(srcT.iterator().next().getValue(), idT.iterator().next().getValue());

		//dynamic not added as target has one
		idT = getIdentitiesByType(result.getIdentities(), PersistentIdentity.ID);
		System.out.println("srcT: " + srcT + "\nidT: " + idT + "\nresult: " + result + 
				"\nmerged: " + mergedFull + "\ntarget: " + targetFull);
		assertEquals(1, idT.size());
		srcT = getIdentitiesByType(targetFull.getIdentities(), PersistentIdentity.ID);
		assertEquals(1, srcT.size());
		assertEquals(srcT.iterator().next().getValue(), idT.iterator().next().getValue());
	}
	
	@Test
	public void groupMembershipsAdded() throws Exception
	{
		Identity target = createUsernameUser("target", InternalAuthorizationManagerImpl.USER_ROLE, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", InternalAuthorizationManagerImpl.USER_ROLE, "p2", CRED_REQ_PASS);

		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		groupsMan.addGroup(new Group("/B/C"));
		
		groupsMan.addMemberFromParent("/A", new EntityParam(target));
		groupsMan.addMemberFromParent("/A", new EntityParam(merged));
		groupsMan.addMemberFromParent("/B", new EntityParam(merged));
		groupsMan.addMemberFromParent("/B/C", new EntityParam(merged));
		
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), false);
		
		Collection<String> groups = idsMan.getGroups(new EntityParam(target)).keySet();
		assertTrue(groups.contains("/A"));
		assertTrue(groups.contains("/B"));
		assertTrue(groups.contains("/B/C"));
	}

	@Test
	public void onlyNewAttributesAdded() throws Exception
	{
		Identity target = createUsernameUser("target", InternalAuthorizationManagerImpl.USER_ROLE, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", InternalAuthorizationManagerImpl.USER_ROLE, "p2", CRED_REQ_PASS);
		
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addMemberFromParent("/A", new EntityParam(merged));
		aTypeMan.addAttributeType(new AttributeType("a", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("b", StringAttributeSyntax.ID));
		attrsMan.createAttribute(new EntityParam(target), StringAttribute.of("a", "/", 
				"v1"));
		attrsMan.createAttribute(new EntityParam(merged), StringAttribute.of("a", "/", 
				"v2"));
		attrsMan.createAttribute(new EntityParam(merged), StringAttribute.of("b", "/", 
				"v1"));
		attrsMan.createAttribute(new EntityParam(merged), StringAttribute.of("a", "/A", 
				"v1"));
		
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), false);
		
		Collection<AttributeExt> a = attrsMan.getAllAttributes(
				new EntityParam(target), false, "/", "a", false);
		assertEquals(1, a.size());
		assertEquals(1, a.iterator().next().getValues().size());
		assertEquals("v1", a.iterator().next().getValues().get(0));
		
		a = attrsMan.getAllAttributes(new EntityParam(target), false, "/", "b", false);
		assertEquals(1, a.size());
		assertEquals(1, a.iterator().next().getValues().size());
		assertEquals("v1", a.iterator().next().getValues().get(0));
		
		a = attrsMan.getAllAttributes(new EntityParam(target), false, "/A", "a", false);
		assertEquals(1, a.size());
		assertEquals(1, a.iterator().next().getValues().size());
		assertEquals("v1", a.iterator().next().getValues().get(0));
	}
	
	@Test
	public void attributeClassesUnchanged() throws Exception
	{
		Identity target = createUsernameUser("target", InternalAuthorizationManagerImpl.USER_ROLE, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", InternalAuthorizationManagerImpl.USER_ROLE, "p2", CRED_REQ_PASS);

		acMan.addAttributeClass(new AttributesClass("acT", "", new HashSet<String>(), 
				new HashSet<String>(), true, new HashSet<String>()));
		acMan.addAttributeClass(new AttributesClass("acM", "", new HashSet<String>(), 
				new HashSet<String>(), true, new HashSet<String>()));
		
		acMan.setEntityAttributeClasses(new EntityParam(target), "/", Sets.newHashSet("acT"));
		acMan.setEntityAttributeClasses(new EntityParam(merged), "/", Sets.newHashSet("acM"));
		
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), false);

		Collection<AttributesClass> ac = acMan.getEntityAttributeClasses(new EntityParam(target), "/");
		assertEquals(1, ac.size());
		assertEquals("acT", ac.iterator().next().getName());
	}

	@Test
	public void credentialRequirementUnchanged() throws Exception
	{
		Identity target = createUsernameUser("target", InternalAuthorizationManagerImpl.USER_ROLE, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", InternalAuthorizationManagerImpl.USER_ROLE, "p2", CRED_REQ_PASS);

		crMan.addCredentialRequirement(new CredentialRequirements("crT", "", new HashSet<String>()));
		crMan.addCredentialRequirement(new CredentialRequirements("crM", "", new HashSet<String>()));
		eCredMan.setEntityCredentialRequirements(new EntityParam(target), "crT");
		eCredMan.setEntityCredentialRequirements(new EntityParam(merged), "crM");
		
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), false);

		Entity entity = idsMan.getEntity(new EntityParam(target));
		assertEquals("crT", entity.getCredentialInfo().getCredentialRequirementId());
	}

	
	@Test
	public void newCredentialAdded() throws Exception
	{
		Identity target = createUsernameUser("target", InternalAuthorizationManagerImpl.USER_ROLE, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", InternalAuthorizationManagerImpl.USER_ROLE, "p2", CRED_REQ_PASS);
		setupPasswordAndCertAuthn();
		crMan.addCredentialRequirement(new CredentialRequirements("pandc", "", Sets.newHashSet(
				"credential2", "credential1")));
		eCredMan.setEntityCredentialRequirements(new EntityParam(target), "pandc");
		eCredMan.setEntityCredentialRequirements(new EntityParam(merged), "pandc");
		
		eCredMan.setEntityCredentialStatus(new EntityParam(target), "credential1", LocalCredentialState.notSet);
		eCredMan.setEntityCredential(new EntityParam(merged), "credential2", "");

		Entity result = idsMan.getEntity(new EntityParam(target.getEntityId()));
		Map<String, CredentialPublicInformation> credentialsState = 
				result.getCredentialInfo().getCredentialsState();
		assertEquals(LocalCredentialState.notSet, credentialsState.get("credential1").getState());
		assertEquals(LocalCredentialState.correct, credentialsState.get("credential2").getState());

		
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), false);

		result = idsMan.getEntity(new EntityParam(target.getEntityId()));
		credentialsState = result.getCredentialInfo().getCredentialsState();
		
		assertEquals(LocalCredentialState.correct, credentialsState.get("credential1").getState());
		assertEquals(LocalCredentialState.correct, credentialsState.get("credential2").getState());
	}

	@Test
	public void identityConflictsDetectedInSafeMode() throws Exception
	{
		Identity target = createUsernameUser("target", null, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", null, "p2", CRED_REQ_PASS);
		eCredMan.setEntityCredentialStatus(new EntityParam(target), "credential1", LocalCredentialState.notSet);
		
		try
		{
			idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), true);
			fail("No error");
		} catch (MergeConflictException e)
		{
			//ok
		}

		idsMan.resetIdentity(new EntityParam(merged.getEntityId()), PersistentIdentity.ID, null, null);
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), true);
	}
	
	@Test
	public void credentialConflictsDetectedInSafeMode() throws Exception
	{
		Identity target = createUsernameUser("target", null, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", null, "p2", CRED_REQ_PASS);
		idsMan.resetIdentity(new EntityParam(merged.getEntityId()), PersistentIdentity.ID, null, null);
		
		try
		{
			idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), true);
			fail("No error");
		} catch (MergeConflictException e)
		{
			//ok
		}

		eCredMan.setEntityCredentialStatus(new EntityParam(target), "credential1", LocalCredentialState.notSet);
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), true);
	}

	@Test
	public void attributeConflictsDetectedInSafeMode() throws Exception
	{
		Identity target = createUsernameUser("target", null, "p1", CRED_REQ_PASS);
		Identity merged = createUsernameUser("merged", null, "p2", CRED_REQ_PASS);
		idsMan.resetIdentity(new EntityParam(merged.getEntityId()), PersistentIdentity.ID, null, null);
		eCredMan.setEntityCredentialStatus(new EntityParam(target), "credential1", LocalCredentialState.notSet);
		aTypeMan.addAttributeType(new AttributeType("a", StringAttributeSyntax.ID));
		attrsMan.createAttribute(new EntityParam(target), StringAttribute.of("a", "/", "v1"));
		attrsMan.createAttribute(new EntityParam(merged), StringAttribute.of("a", "/", "v2"));

		try
		{
			idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), true);
			fail("No error");
		} catch (MergeConflictException e)
		{
			//ok
		}

		attrsMan.removeAttribute(new EntityParam(merged), "/", "a");
		idsMan.mergeEntities(new EntityParam(target), new EntityParam(merged), true);
	}

}
