/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteVerificatorUtil;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.InputTranslationActionsRegistry;
import pl.edu.icm.unity.server.registries.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.server.translation.in.GroupEffectMode;
import pl.edu.icm.unity.server.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationRule;
import pl.edu.icm.unity.server.translation.in.MappedAttribute;
import pl.edu.icm.unity.server.translation.in.MappedGroup;
import pl.edu.icm.unity.server.translation.in.MappedIdentity;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.translation.in.action.BlindStopperInputAction;
import pl.edu.icm.unity.server.translation.in.action.EntityChangeActionFactory;
import pl.edu.icm.unity.server.translation.in.action.MapAttributeActionFactory;
import pl.edu.icm.unity.server.translation.in.action.MapGroupActionFactory;
import pl.edu.icm.unity.server.translation.in.action.MapIdentityActionFactory;
import pl.edu.icm.unity.server.translation.in.action.RemoveStaleDataActionFactory;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationRule;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.translation.out.CreateAttributeActionFactory;
import pl.edu.icm.unity.stdext.translation.out.CreatePersistentAttributeActionFactory;
import pl.edu.icm.unity.stdext.translation.out.CreatePersistentIdentityActionFactory;
import pl.edu.icm.unity.stdext.translation.out.FilterAttributeActionFactory;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Integration and engine related part tests of the subsystem mapping the remote data to the unity's representation. 
 * @author K. Benedyczak
 */
public class TestTranslationProfiles extends DBIntegrationTestBase
{
	@Autowired
	protected TranslationProfileManagement tprofMan;
	@Autowired
	protected InputTranslationActionsRegistry intactionReg;
	@Autowired
	protected OutputTranslationActionsRegistry outtactionReg;
	@Autowired
	protected InputTranslationEngine inputTrEngine;
	@Autowired
	protected OutputTranslationEngine outputTrEngine;
	
	@Test
	public void testInputPersistence() throws Exception
	{
		assertEquals(0, tprofMan.listInputProfiles().size());
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'joe'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = intactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'"); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		
		InputTranslationProfile toAdd = new InputTranslationProfile("p1", rules, intactionReg);
		tprofMan.addProfile(toAdd);
		
		Map<String, InputTranslationProfile> profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(MapIdentityActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals(IdentifierIdentity.ID, profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		rules.remove(0);
		toAdd = new InputTranslationProfile("p1", rules, intactionReg);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(MapGroupActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals("'/A'", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		tprofMan.removeProfile("p1");
		assertEquals(0, tprofMan.listInputProfiles().size());
	}

	@Test
	public void testOutputPersistence() throws Exception
	{
		assertEquals(0, tprofMan.listInputProfiles().size());
		List<OutputTranslationRule> rules = new ArrayList<>();
		OutputTranslationAction action1 = outtactionReg.getByName(CreateAttributeActionFactory.NAME).getInstance(
				"dynAttr", "'joe'", "false");
		rules.add(new OutputTranslationRule(action1, new TranslationCondition()));
		OutputTranslationAction action2 = outtactionReg.getByName(FilterAttributeActionFactory.NAME).getInstance(
				"attr"); 
		rules.add(new OutputTranslationRule(action2, new TranslationCondition()));
		
		OutputTranslationProfile toAdd = new OutputTranslationProfile("p1", rules, outtactionReg);
		tprofMan.addProfile(toAdd);
		
		Map<String, OutputTranslationProfile> profiles = tprofMan.listOutputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(CreateAttributeActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals("dynAttr", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		assertEquals("'joe'", profiles.get("p1").getRules().get(0).getAction().getParameters()[1]);
		
		rules.remove(0);
		toAdd = new OutputTranslationProfile("p1", rules, outtactionReg);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listOutputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(FilterAttributeActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals("attr", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		tprofMan.removeProfile("p1");
		assertEquals(0, tprofMan.listOutputProfiles().size());
	}
	
	@Test
	public void testIntegratedInput() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				X500Identity.ID, 
				"'CN=' + attr['cn'] + ',O=ICM,UID=' + id", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = intactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'", GroupEffectMode.REQUIRE_EXISTING_GROUP.name()); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		InputTranslationAction action2prim = intactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A/newGr'", GroupEffectMode.CREATE_GROUP_IF_MISSING.name()); 
		rules.add(new InputTranslationRule(action2prim, new TranslationCondition()));
		InputTranslationAction action3 = intactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"o", "/A", "groups",
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString()); 
		rules.add(new InputTranslationRule(action3, new TranslationCondition()));
		InputTranslationAction action4 = intactionReg.getByName(EntityChangeActionFactory.NAME).getInstance(
				EntityScheduledOperation.REMOVE.toString(), "1"); 
		rules.add(new InputTranslationRule(action4, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, intactionReg);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("someUser", UsernameIdentity.ID));
		input.addAttribute(new RemoteAttribute("cn", "foo"));
		input.addGroup(new RemoteGroupMembership("mimuw"));
		input.addGroup(new RemoteGroupMembership("icm"));
		
		MappingResult result = tp1.translate(input);
		inputTrEngine.process(result);
		
		EntityParam ep = new EntityParam(new IdentityTaV(X500Identity.ID, "CN=foo,O=ICM,UID=someUser"));
		Entity entity = idsMan.getEntity(ep);
		assertEquals(EntityScheduledOperation.REMOVE, 
				entity.getEntityInformation().getScheduledOperation());
		
		long nextDay = System.currentTimeMillis() + 3600L*24*1000;
		assertTrue(nextDay >= entity.getEntityInformation().getScheduledOperationTime().getTime());
		assertTrue(nextDay - 1000 < entity.getEntityInformation().getScheduledOperationTime().getTime());
		assertEquals(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				entity.getCredentialInfo().getCredentialRequirementId());
		assertEquals(2, entity.getIdentities().length);
		Identity id = getIdentityByType(entity.getIdentities(), X500Identity.ID);
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertEquals("test", id.getRemoteIdp());
		assertEquals("p1", id.getTranslationProfile());
		Collection<AttributeExt<?>> atrs = attrsMan.getAttributes(ep, "/A", "o");
		assertEquals(1, atrs.size());
		AttributeExt<?> at = atrs.iterator().next();
		assertEquals(2, at.getValues().size());
		assertEquals("mimuw", at.getValues().get(0));
		assertEquals("icm", at.getValues().get(1));
		assertNotNull(at.getCreationTs());
		assertNotNull(at.getUpdateTs());
		assertEquals("test", at.getRemoteIdp());
		assertEquals("p1", at.getTranslationProfile());
		
		Map<String, GroupMembership> groups = idsMan.getGroups(ep);
		assertTrue(groups.containsKey("/A"));
		assertEquals("test", groups.get("/A").getRemoteIdp());
		assertEquals("p1", groups.get("/A").getTranslationProfile());
		assertNotNull(groups.get("/A").getCreationTs());
		assertTrue(groups.containsKey("/A/newGr"));
		assertEquals("test", groups.get("/A/newGr").getRemoteIdp());
		assertEquals("p1", groups.get("/A/newGr").getTranslationProfile());
		assertNotNull(groups.get("/A/newGr").getCreationTs());
	}

	@Test
	public void staleDataRemoved() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		
		EntityParam ep = new EntityParam(new IdentityTaV(IdentifierIdentity.ID, "id"));
		idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "id"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		idsMan.addIdentity(new IdentityParam(IdentifierIdentity.ID, "id2", "test", "p1"), ep, false);
		groupsMan.addMemberFromParent("/A", ep, null, "test", "p1");
		groupsMan.addMemberFromParent("/B", ep, null, "test", "p1");
		StringAttribute attr = new StringAttribute("o", "/", AttributeVisibility.full, "v1");
		attr.setRemoteIdp("test");
		attr.setTranslationProfile("p1");
		attrsMan.setAttribute(ep, attr, false);
		
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'id'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = intactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'", GroupEffectMode.REQUIRE_EXISTING_GROUP.name()); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		InputTranslationAction action3 = intactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"o", "/A", "['groups']",
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString()); 
		rules.add(new InputTranslationRule(action3, new TranslationCondition()));
		InputTranslationAction action4 = intactionReg.getByName(RemoveStaleDataActionFactory.NAME).getInstance(); 
		rules.add(new InputTranslationRule(action4, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, intactionReg);
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		MappingResult result = tp1.translate(input);
		inputTrEngine.process(result);

		
		Entity entity = idsMan.getEntity(ep);
		
		assertEquals(1, getIdentitiesByType(entity.getIdentities(), IdentifierIdentity.ID).size());
		Identity id = getIdentityByType(entity.getIdentities(), IdentifierIdentity.ID);
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertNull(id.getRemoteIdp());
		assertNull(id.getTranslationProfile());
		assertEquals("id", id.getValue());
		
		Collection<AttributeExt<?>> atrs = attrsMan.getAttributes(ep, "/A", "o");
		assertEquals(1, atrs.size());
		AttributeExt<?> at = atrs.iterator().next();
		assertEquals(1, at.getValues().size());
		assertEquals("groups", at.getValues().get(0));
		assertNotNull(at.getCreationTs());
		assertNotNull(at.getUpdateTs());
		assertEquals("test", at.getRemoteIdp());
		assertEquals("p1", at.getTranslationProfile());

		atrs = attrsMan.getAttributes(ep, "/", "o");
		assertEquals(0, atrs.size());
		
		Map<String, GroupMembership> groups = idsMan.getGroups(ep);
		assertTrue(groups.containsKey("/A"));
		assertEquals("test", groups.get("/A").getRemoteIdp());
		assertEquals("p1", groups.get("/A").getTranslationProfile());
		assertNotNull(groups.get("/A").getCreationTs());
		assertFalse(groups.containsKey("/B"));
	}

	
	
	@Test
	public void testInputCreateOrUpdateIdentityMapping() throws Exception
	{
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'test'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.UPDATE_OR_MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'test-base'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString());
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, intactionReg);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		MappingResult result = tp1.translate(input);
		inputTrEngine.process(result);

		EntityParam ep = new EntityParam(new IdentityTaV(IdentifierIdentity.ID, "test"));
		EntityParam ep2 = new EntityParam(new IdentityTaV(IdentifierIdentity.ID, "test-base"));
		try
		{
			idsMan.getEntity(ep);
			fail("Entity created");
		} catch (IllegalIdentityValueException e)
		{
			//ok
		}
		try
		{
			idsMan.getEntity(ep2);
			fail("Entity created");
		} catch (IllegalIdentityValueException e)
		{
			//ok
		}
		
		idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "test"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid, false);

		
		MappingResult result2 = tp1.translate(input);
		inputTrEngine.process(result2);

		try
		{
			idsMan.getEntity(ep);
		} catch (IllegalIdentityValueException e)
		{
			fail("Entity not created");
		}
	}

	@Test
	public void testConfirmationStateSettingFromProfile() throws Exception
	{
		AttributeType oType = new AttributeType("email", new VerifiableEmailAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				EmailIdentity.ID, 
				"'id1@example.com[CONFIRMED]'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 =  intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				EmailIdentity.ID, 
				"'id2@example.com[UNCONFIRMED]'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		InputTranslationAction action3 =  intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				EmailIdentity.ID, 
				"'id3@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()); 
		rules.add(new InputTranslationRule(action3, new TranslationCondition()));
		InputTranslationAction action4 = intactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"email", "/", "['id4@example.com[CONFIRMED]', 'id5@example.com[UNCONFIRMED]', 'id6@example.com']",
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString()); 
		rules.add(new InputTranslationRule(action4, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, intactionReg);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		
		MappingResult result = tp1.translate(input);
		inputTrEngine.process(result);
		
		EntityParam ep = new EntityParam(new IdentityTaV(EmailIdentity.ID, "id1@example.com"));
		Entity entity = idsMan.getEntity(ep);
		int num = 0;
		for (Identity id: entity.getIdentities())
		{
			if (id.getTypeId().equals(EmailIdentity.ID))
			{
				if ("id1@example.com".equals(id.getValue()))
				{
					assertTrue(id.isConfirmed());
					num ++;
				} else if ("id2@example.com".equals(id.getValue()))
				{
					assertFalse(id.isConfirmed());
					num ++;
					
				} else if ("id3@example.com".equals(id.getValue()))
				{
					assertFalse(id.isConfirmed());
					num ++;
				}
				
			}
		}
		assertEquals(3, num);
		
		
		Collection<AttributeExt<?>> atrs = attrsMan.getAttributes(ep, "/", "email");
		assertEquals(1, atrs.size());
		AttributeExt<?> at = atrs.iterator().next();
		assertEquals(3, at.getValues().size());
		assertEquals("id4@example.com", at.getValues().get(0).toString());
		assertTrue(((VerifiableElement)at.getValues().get(0)).isConfirmed());
		assertEquals("id5@example.com", at.getValues().get(1).toString());
		assertFalse(((VerifiableElement)at.getValues().get(1)).isConfirmed());
		assertEquals("id6@example.com", at.getValues().get(2).toString());
		assertFalse(((VerifiableElement)at.getValues().get(2)).isConfirmed());
	}

	
	
	@Test
	public void testIntegratedInputWithReg() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				X500Identity.ID, 
				"'CN=' + attr['cn'] + ',O=ICM,UID=' + id", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = intactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'"); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		InputTranslationAction action3 = intactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"o", "/A", "groups",
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString()); 
		rules.add(new InputTranslationRule(action3, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, intactionReg);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("someUser", UsernameIdentity.ID));
		input.addAttribute(new RemoteAttribute("cn", "foo"));
		input.addGroup(new RemoteGroupMembership("mimuw"));
		input.addGroup(new RemoteGroupMembership("icm"));
		
		MappingResult result = tp1.translate(input);
		inputTrEngine.process(result);
		
		assertEquals(1, result.getIdentities().size());
		IdentityParam ep = new IdentityParam(X500Identity.ID, "CN=foo,O=ICM,UID=someUser");
		ep.setRemoteIdp("test");
		ep.setTranslationProfile("p1");
		assertEquals(ep, result.getIdentities().get(0).getIdentity());
		
		assertEquals(1, result.getGroups().size());
		assertEquals("/A", result.getGroups().get(0).getGroup());
		assertEquals("test", result.getGroups().get(0).getIdp());
		assertEquals("p1", result.getGroups().get(0).getProfile());
		
		assertEquals(1, result.getAttributes().size());
		assertEquals("o", result.getAttributes().get(0).getAttribute().getName());
		assertEquals(2, result.getAttributes().get(0).getAttribute().getValues().size());
	}
	
	
	@Test
	public void testIntegratedOutput() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		Identity user = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1234"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid, false);
		Entity userE = idsMan.getEntity(new EntityParam(user));
		
		List<OutputTranslationRule> rules = new ArrayList<>();
		OutputTranslationAction action1 = outtactionReg.getByName(
				CreatePersistentIdentityActionFactory.NAME).getInstance(
						X500Identity.ID, 
						"'CN=foo,O=ICM,DC=' + authenticatedWith[0]");
		rules.add(new OutputTranslationRule(action1, new TranslationCondition()));
		OutputTranslationAction action2 = outtactionReg.getByName(
				CreatePersistentAttributeActionFactory.NAME).getInstance(
						"o", "'ICM'", "false", "/"); 
		rules.add(new OutputTranslationRule(action2, new TranslationCondition()));

		OutputTranslationProfile tp1 = new OutputTranslationProfile("p1", rules, outtactionReg);
		
		setupPasswordAuthn();
		createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		setupUserContext("user1", false);
		InvocationContext.getCurrent().getLoginSession().addAuthenticatedIdentities(Sets.newHashSet("user1"));
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), userE, 
				"/", Collections.singleton("/"),
				"req", "proto", "subProto");
		
		TranslationResult result = tp1.translate(input);
		outputTrEngine.process(input, result);
		
		setupAdmin();
		
		EntityParam ep = new EntityParam(new IdentityTaV(X500Identity.ID, "CN=foo,O=ICM,DC=user1"));
		Entity entity = idsMan.getEntity(ep);
		assertEquals(userE.getId(), entity.getId());
		assertEquals(3, entity.getIdentities().length);
		Identity id = getIdentityByType(entity.getIdentities(), X500Identity.ID);
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertEquals("p1", id.getTranslationProfile());
		
		Collection<AttributeExt<?>> atrs = attrsMan.getAttributes(ep, "/", "o");
		assertEquals(1, atrs.size());
		AttributeExt<?> at = atrs.iterator().next();
		assertEquals(1, at.getValues().size());
		assertEquals("ICM", at.getValues().get(0));
		assertNotNull(at.getCreationTs());
		assertNotNull(at.getUpdateTs());
		assertEquals("p1", at.getTranslationProfile());
	}

	@Test
	public void outputTranslationProducesStringAttributes() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		AttributeType eType = new AttributeType("e", new VerifiableEmailAttributeSyntax());
		attrsMan.addAttributeType(eType);
		AttributeType fType = new AttributeType("f", new FloatingPointAttributeSyntax());
		attrsMan.addAttributeType(fType);
		
		setupPasswordAuthn();
		Identity user = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		Entity userE = idsMan.getEntity(new EntityParam(user));
		
		List<OutputTranslationRule> rules = new ArrayList<>();
		OutputTranslationAction action1 = outtactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
						"a1", "attr['o']", "false");
		rules.add(new OutputTranslationRule(action1, new TranslationCondition()));
		OutputTranslationAction action2 = outtactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
						"a2", "attr['e']","false"); 
		rules.add(new OutputTranslationRule(action2, new TranslationCondition()));
		OutputTranslationAction action3 = outtactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
						"a3", "attr['f']","false"); 
		rules.add(new OutputTranslationRule(action3, new TranslationCondition()));

		OutputTranslationProfile tp1 = new OutputTranslationProfile("p1", rules, outtactionReg);
		
		setupUserContext("user1", false);
		InvocationContext.getCurrent().getLoginSession().addAuthenticatedIdentities(Sets.newHashSet("user1"));
		
		@SuppressWarnings("unchecked")
		TranslationInput input = new TranslationInput(
				Lists.newArrayList(
					new StringAttribute("o", "/", AttributeVisibility.full, "v1"),
					new VerifiableEmailAttribute("e", "/", 
						AttributeVisibility.full, "email@example.com"),
					new FloatingPointAttribute("f", "/", 
						AttributeVisibility.full, 123)), 
				userE, 
				"/", Collections.singleton("/"),
				"req", "proto", "subProto");
		
		TranslationResult result = tp1.translate(input);

		Collection<DynamicAttribute> attributes = result.getAttributes();
		assertThat(attributes.size(), is(6));
		for (DynamicAttribute da: attributes)
		{
			Attribute<?> a = da.getAttribute();
			if (a.getName().startsWith("a"))
			{
				assertThat(a.getAttributeSyntax().getValueSyntaxId(), 
						is(StringAttributeSyntax.ID));
				for (Object val: a.getValues())
					assertThat(val, is(instanceOf(String.class)));
			}
		}
	}
	
	@Test
	public void testManualMergeWithExisting() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));

		MappingResult result = new MappingResult();
		result.addAttribute(new MappedAttribute(AttributeEffectMode.CREATE_ONLY, new StringAttribute("o", 
				"/A", AttributeVisibility.full, "org")));
		result.addGroup(new MappedGroup("/A", GroupEffectMode.ADD_IF_GROUP_EXISTS, "idp", "profile"));
		result.addIdentity(new MappedIdentity(IdentityEffectMode.CREATE_OR_MATCH, 
				new IdentityParam(UsernameIdentity.ID, "added"), "dummy"));
		
		setupPasswordAuthn();
		Identity baseUser = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam baseUserP = new EntityParam(baseUser);
		
		inputTrEngine.mergeWithExisting(result, baseUserP);
		
		Entity entity = idsMan.getEntity(baseUserP);
		boolean hasBase = false;
		boolean hasNew = false;
		for (Identity id: entity.getIdentities())
		{
			if (id.getTypeId().equals(UsernameIdentity.ID))
			{
				if ("user1".equals(id.getValue()))
				{
					hasBase = true;
				} else if ("added".equals(id.getValue()))
				{
					hasNew = true;
				}
			}
		}
		assertTrue("New not added", hasNew);
		assertTrue("Old not preserved", hasBase);
		
		Collection<AttributeExt<?>> atrs = attrsMan.getAttributes(baseUserP, "/A", "o");
		assertEquals(1, atrs.size());
		AttributeExt<?> at = atrs.iterator().next();
		assertEquals(1, at.getValues().size());
		assertEquals("org", at.getValues().get(0).toString());
	}

	
	@Test
	public void primaryIdentityProperlySet() throws Exception
	{
		Identity toBeMappedOn = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "known"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				EntityState.valid, false);
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'unknown'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'known'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString());
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, intactionReg);
		tprofMan.addProfile(tp1);
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		
		RemoteVerificatorUtil verificatatorUtil = new RemoteVerificatorUtil(identityResolver, 
				tprofMan, inputTrEngine);
		RemotelyAuthenticatedContext processed = verificatatorUtil.processRemoteInput(input, "p1", false);

		assertNotNull(processed.getLocalMappedPrincipal());
		assertEquals(toBeMappedOn.getEntityId(), processed.getLocalMappedPrincipal().getEntityId());
	}
	
	@Test
	public void emailTagsArePreserved() throws Exception
	{
		attrsMan.addAttributeType(new AttributeType("email", new VerifiableEmailAttributeSyntax()));
		
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = intactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				EmailIdentity.ID, 
				"'a+tag@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = intactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"email", 
				"/",
				"'b+tag@example.com'",
				AttributeVisibility.full.toString(), 
				AttributeEffectMode.CREATE_OR_UPDATE.toString());
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, intactionReg);
		tprofMan.addProfile(tp1);
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");

		MappingResult result = tp1.translate(input);
		inputTrEngine.process(result);
		
		EntityParam ep = new EntityParam(new IdentityTaV(EmailIdentity.ID, "a@example.com"));
		Entity entity = idsMan.getEntity(ep);
		boolean hasTagged = Stream.of(entity.getIdentities()).
				map(i -> i.getValue()).
				anyMatch(iv -> iv.equals("a+tag@example.com"));
		assertThat(hasTagged, is(true));
		
		Collection<AttributeExt<?>> attrs = attrsMan.getAllAttributes(ep, false, "/", "email", true);
		assertThat(attrs.size(), is(1));
		List<?> values = attrs.iterator().next().getValues();
		assertThat(values.size(), is(1));
		assertThat(((VerifiableEmail)values.get(0)).getValue(), is("b+tag@example.com"));
	}
	
	
	@Test
	public void profileWithStaleActionsIsLoaded() throws Exception
	{
		attrsMan.addAttributeType(new AttributeType("someAttr", new StringAttributeSyntax()));
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action = (InputTranslationAction) intactionReg.getByName(
				MapAttributeActionFactory.NAME).getInstance("someAttr", "/", "'val'", "full", 
						"CREATE_ONLY"); 
		rules.add(new InputTranslationRule(action, new TranslationCondition()));
		
		TranslationProfile toAdd = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		tprofMan.addProfile(toAdd);
		
		Map<String, InputTranslationProfile> profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		
		attrsMan.removeAttributeType("someAttr", true);
		
		profiles = tprofMan.listInputProfiles();
		InputTranslationProfile retProfile = profiles.get("p1");
		assertNotNull(retProfile);
		assertEquals(1, retProfile.getRules().size());
		TranslationAction firstAction = retProfile.getRules().get(0).getAction();
		assertEquals(MapAttributeActionFactory.NAME, firstAction.getName());
		
		InputTranslationProfile profileInstance = new InputTranslationProfile(toAdd.getName(), rules, intactionReg);
		TranslationActionInstance firstActionInstance = profileInstance.getRuleInstances().get(0).getActionInstance();
		assertThat(firstActionInstance, is(instanceOf(BlindStopperInputAction.class)));
	}
}



















