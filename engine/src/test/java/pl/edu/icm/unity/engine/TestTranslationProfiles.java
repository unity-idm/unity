/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.server.translation.in.GroupEffectMode;
import pl.edu.icm.unity.server.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile.ProfileMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationRule;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationRule;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.tactions.in.EntityChangeActionFactory;
import pl.edu.icm.unity.stdext.tactions.in.MapAttributeActionFactory;
import pl.edu.icm.unity.stdext.tactions.in.MapGroupActionFactory;
import pl.edu.icm.unity.stdext.tactions.in.MapIdentityActionFactory;
import pl.edu.icm.unity.stdext.tactions.out.CreateAttributeActionFactory;
import pl.edu.icm.unity.stdext.tactions.out.CreatePersistentAttributeActionFactory;
import pl.edu.icm.unity.stdext.tactions.out.CreatePersistentIdentityActionFactory;
import pl.edu.icm.unity.stdext.tactions.out.FilterAttributeActionFactory;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Integration and engine related part tests of the subsystem mapping the remote data to the unity's representation. 
 * @author K. Benedyczak
 */
public class TestTranslationProfiles extends DBIntegrationTestBase
{
	@Autowired
	protected TranslationProfileManagement tprofMan;
	@Autowired
	protected TranslationActionsRegistry tactionReg;
	@Autowired
	protected InputTranslationEngine inputTrEngine;
	@Autowired
	protected OutputTranslationEngine outputTrEngine;
	
	@Test
	public void testInputPersistence() throws Exception
	{
		assertEquals(0, tprofMan.listInputProfiles().size());
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = (InputTranslationAction) tactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'joe'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = (InputTranslationAction) tactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'"); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		
		InputTranslationProfile toAdd = new InputTranslationProfile("p1", rules, ProfileMode.UPDATE_ONLY);
		tprofMan.addProfile(toAdd);
		
		Map<String, InputTranslationProfile> profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(MapIdentityActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getActionDescription().getName());
		assertEquals(IdentifierIdentity.ID, profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		rules.remove(0);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(MapGroupActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getActionDescription().getName());
		assertEquals("'/A'", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		tprofMan.removeProfile("p1");
		assertEquals(0, tprofMan.listInputProfiles().size());
	}

	@Test
	public void testOutputPersistence() throws Exception
	{
		assertEquals(0, tprofMan.listInputProfiles().size());
		List<OutputTranslationRule> rules = new ArrayList<>();
		OutputTranslationAction action1 = (OutputTranslationAction) tactionReg.getByName(CreateAttributeActionFactory.NAME).getInstance(
				"dynAttr", 
				"'joe'");
		rules.add(new OutputTranslationRule(action1, new TranslationCondition()));
		OutputTranslationAction action2 = (OutputTranslationAction) tactionReg.getByName(FilterAttributeActionFactory.NAME).getInstance(
				"attr"); 
		rules.add(new OutputTranslationRule(action2, new TranslationCondition()));
		
		OutputTranslationProfile toAdd = new OutputTranslationProfile("p1", rules);
		tprofMan.addProfile(toAdd);
		
		Map<String, OutputTranslationProfile> profiles = tprofMan.listOutputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(CreateAttributeActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getActionDescription().getName());
		assertEquals("dynAttr", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		assertEquals("'joe'", profiles.get("p1").getRules().get(0).getAction().getParameters()[1]);
		
		rules.remove(0);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listOutputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(FilterAttributeActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getActionDescription().getName());
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
		InputTranslationAction action1 = (InputTranslationAction) tactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				X500Identity.ID, 
				"'CN=' + attr['cn'] + ',O=ICM,UID=' + id", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = (InputTranslationAction) tactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'", GroupEffectMode.REQUIRE_EXISTING_GROUP.name()); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		InputTranslationAction action2prim = (InputTranslationAction) tactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A/newGr'", GroupEffectMode.CREATE_GROUP_IF_MISSING.name()); 
		rules.add(new InputTranslationRule(action2prim, new TranslationCondition()));
		InputTranslationAction action3 = (InputTranslationAction) tactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"o", "/A", "groups",
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString()); 
		rules.add(new InputTranslationRule(action3, new TranslationCondition()));
		InputTranslationAction action4 = (InputTranslationAction) tactionReg.getByName(EntityChangeActionFactory.NAME).getInstance(
				EntityScheduledOperation.FORCED_REMOVAL.toString(), "1000"); 
		rules.add(new InputTranslationRule(action4, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, ProfileMode.UPDATE_ONLY);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("someUser", UsernameIdentity.ID));
		input.addAttribute(new RemoteAttribute("cn", "foo"));
		input.addGroup(new RemoteGroupMembership("mimuw"));
		input.addGroup(new RemoteGroupMembership("icm"));
		
		MappingResult result = tp1.translate(input);
		inputTrEngine.process(result);
		
		EntityParam ep = new EntityParam(new IdentityTaV(X500Identity.ID, "CN=foo,O=ICM,UID=someUser"));
		Entity entity = idsMan.getEntity(ep);
		assertEquals(EntityScheduledOperation.FORCED_REMOVAL, 
				entity.getEntityInformation().getScheduledOperation());
		assertEquals(new Date(1000), 
				entity.getEntityInformation().getScheduledOperationTime());
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
		
		Collection<String> groups = idsMan.getGroups(ep);
		assertTrue(groups.contains("/A"));
		assertTrue(groups.contains("/A/newGr"));
	}

	
	@Test
	public void testIntegratedInputWithReg() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		List<InputTranslationRule> rules = new ArrayList<>();
		InputTranslationAction action1 = (InputTranslationAction) tactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				X500Identity.ID, 
				"'CN=' + attr['cn'] + ',O=ICM,UID=' + id", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString());
		rules.add(new InputTranslationRule(action1, new TranslationCondition()));
		InputTranslationAction action2 = (InputTranslationAction) tactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'"); 
		rules.add(new InputTranslationRule(action2, new TranslationCondition()));
		InputTranslationAction action3 = (InputTranslationAction) tactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"o", "/A", "groups",
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString()); 
		rules.add(new InputTranslationRule(action3, new TranslationCondition()));
		
		InputTranslationProfile tp1 = new InputTranslationProfile("p1", rules, ProfileMode.UPDATE_ONLY);
		
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
		OutputTranslationAction action1 = (OutputTranslationAction) tactionReg.getByName(
				CreatePersistentIdentityActionFactory.NAME).getInstance(
						X500Identity.ID, 
						"'CN=foo,O=ICM'");
		rules.add(new OutputTranslationRule(action1, new TranslationCondition()));
		OutputTranslationAction action2 = (OutputTranslationAction) tactionReg.getByName(
				CreatePersistentAttributeActionFactory.NAME).getInstance(
						"o", "'ICM'", "/"); 
		rules.add(new OutputTranslationRule(action2, new TranslationCondition()));
		
		OutputTranslationProfile tp1 = new OutputTranslationProfile("p1", rules);
		
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), userE, 
				"/", Collections.singleton("/"),
				"req", "proto", "subProto");
		
		TranslationResult result = tp1.translate(input);
		outputTrEngine.process(input, result);
		
		EntityParam ep = new EntityParam(new IdentityTaV(X500Identity.ID, "CN=foo,O=ICM"));
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

}



















