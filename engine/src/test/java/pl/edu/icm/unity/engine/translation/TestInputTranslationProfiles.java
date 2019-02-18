/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.GroupEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappedAttribute;
import pl.edu.icm.unity.engine.api.translation.in.MappedGroup;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.in.action.BlindStopperInputAction;
import pl.edu.icm.unity.engine.translation.in.action.EntityChangeActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.IncludeInputProfileActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapGroupActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapIdentityActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.RemoveStaleDataActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Integration and engine related part tests of the subsystem mapping the remote data to the unity's representation. 
 * @author K. Benedyczak
 */
public class TestInputTranslationProfiles extends DBIntegrationTestBase
{
	@Autowired
	private TranslationProfileManagement tprofMan;
	@Autowired
	private InputTranslationEngine inputTrEngine;
	@Autowired
	private RemoteAuthnResultProcessor remoteProcessor;
	@Autowired
	private InputTranslationActionsRegistry intactionReg;
	@Autowired
	private TransactionalRunner tx;
	@Autowired
	InputTranslationProfileRepository inputProfileRepo;
	
	@Test
	public void testInputPersistence() throws Exception
	{
		assertThat(listDefaultModeProfiles().size(), is(0));
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction( 
				MapIdentityActionFactory.NAME, new String[] {
				IdentifierIdentity.ID, 
				"'joe'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(MapGroupActionFactory.NAME, new String[] {
				"'/A'"}); 
		rules.add(new TranslationRule("true", action2));
		
		TranslationProfile toAdd = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		tprofMan.addProfile(toAdd);
		
		Map<String, TranslationProfile> profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(MapIdentityActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals(IdentifierIdentity.ID, profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		rules.remove(0);
		toAdd = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(MapGroupActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals("'/A'", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		tprofMan.removeProfile(ProfileType.INPUT, "p1");
		assertThat(listDefaultModeProfiles().size(), is(0));
	}
	
	private List<TranslationProfile> listDefaultModeProfiles() throws EngineException
	{
		return tprofMan.listInputProfiles().values().stream()
				.filter(t -> t.getProfileMode() == ProfileMode.DEFAULT)
				.collect(Collectors.toList());
	}

	@Test
	public void testIntegratedInput() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				X500Identity.ID, 
				"'CN=' + attr['cn'] + ',O=ICM,UID=' + id", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(MapGroupActionFactory.NAME, new String[] {
				"'/A'", GroupEffectMode.REQUIRE_EXISTING_GROUP.name()}); 
		rules.add(new TranslationRule("true", action2));
		TranslationAction action2prim = new TranslationAction(MapGroupActionFactory.NAME, new String[] {
				"'/A/newGr'", GroupEffectMode.CREATE_GROUP_IF_MISSING.name()}); 
		rules.add(new TranslationRule("true", action2prim));
		TranslationAction action3 = new TranslationAction(MapAttributeActionFactory.NAME, new String[] {
				"o", "/A", "groups",
				AttributeEffectMode.CREATE_OR_UPDATE.toString()}); 
		rules.add(new TranslationRule("true", action3));
		TranslationAction action4 = new TranslationAction(EntityChangeActionFactory.NAME, new String[] {
				EntityScheduledOperation.REMOVE.toString(), "1"}); 
		rules.add(new TranslationRule("true", action4));
		
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("someUser", UsernameIdentity.ID));
		input.addAttribute(new RemoteAttribute("cn", "foo"));
		input.addGroup(new RemoteGroupMembership("mimuw"));
		input.addGroup(new RemoteGroupMembership("icm"));

		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo,intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});
		
		EntityParam ep = new EntityParam(new IdentityTaV(X500Identity.ID, "CN=foo,O=ICM,UID=someUser"));
		Entity entity = idsMan.getEntity(ep);
		assertEquals(EntityScheduledOperation.REMOVE, 
				entity.getEntityInformation().getScheduledOperation());
		
		long nextDay = System.currentTimeMillis() + 3600L*24*1000;
		assertTrue(nextDay >= entity.getEntityInformation().getScheduledOperationTime().getTime());
		assertTrue(nextDay - 1000 < entity.getEntityInformation().getScheduledOperationTime().getTime());
		assertEquals(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				entity.getCredentialInfo().getCredentialRequirementId());
		assertEquals(2, entity.getIdentities().size());
		Identity id = getIdentityByType(entity.getIdentities(), X500Identity.ID);
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertEquals("test", id.getRemoteIdp());
		assertEquals("p1", id.getTranslationProfile());
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/A", "o");
		assertEquals(1, atrs.size());
		AttributeExt at = atrs.iterator().next();
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
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		
		EntityParam ep = new EntityParam(new IdentityTaV(IdentifierIdentity.ID, "id"));
		idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "id"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		idsMan.addIdentity(new IdentityParam(IdentifierIdentity.ID, "id2", "test", "p1"), ep, false);
		groupsMan.addMemberFromParent("/A", ep, null, "test", "p1");
		groupsMan.addMemberFromParent("/B", ep, null, "test", "p1");
		Attribute attr = StringAttribute.of("o", "/", "v1");
		attr.setRemoteIdp("test");
		attr.setTranslationProfile("p1");
		attrsMan.createAttribute(ep, attr);
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				IdentifierIdentity.ID, 
				"'id'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(MapGroupActionFactory.NAME, new String[] {
				"'/A'", GroupEffectMode.REQUIRE_EXISTING_GROUP.name()}); 
		rules.add(new TranslationRule("true", action2));
		TranslationAction action3 = new TranslationAction(MapAttributeActionFactory.NAME, new String[] {
				"o", "/A", "['groups']",
				AttributeEffectMode.CREATE_OR_UPDATE.toString()}); 
		rules.add(new TranslationRule("true", action3));
		TranslationAction action4 = new TranslationAction(RemoveStaleDataActionFactory.NAME, new String[] {}); 
		rules.add(new TranslationRule("true", action4));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");

		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});
		
		Entity entity = idsMan.getEntity(ep);
		
		assertEquals(1, getIdentitiesByType(entity.getIdentities(), IdentifierIdentity.ID).size());
		Identity id = getIdentityByType(entity.getIdentities(), IdentifierIdentity.ID);
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertNull(id.getRemoteIdp());
		assertNull(id.getTranslationProfile());
		assertEquals("id", id.getValue());
		
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/A", "o");
		assertEquals(1, atrs.size());
		AttributeExt at = atrs.iterator().next();
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
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				IdentifierIdentity.ID, 
				"'test'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.UPDATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				IdentifierIdentity.ID, 
				"'test-base'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString()});
		rules.add(new TranslationRule("true", action2));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");

		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});

		EntityParam ep = new EntityParam(new IdentityTaV(IdentifierIdentity.ID, "test"));
		EntityParam ep2 = new EntityParam(new IdentityTaV(IdentifierIdentity.ID, "test-base"));
		try
		{
			idsMan.getEntity(ep);
			fail("Entity created");
		} catch (IllegalArgumentException e)
		{
			//ok
		}
		try
		{
			idsMan.getEntity(ep2);
			fail("Entity created");
		} catch (IllegalArgumentException e)
		{
			//ok
		}
		
		idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "test"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid, false);

		
		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});

		try
		{
			idsMan.getEntity(ep);
		} catch (IllegalArgumentException e)
		{
			fail("Entity not created");
		}
	}

	@Test
	public void testConfirmationStateSettingFromProfile() throws Exception
	{
		AttributeType oType = new AttributeType("email", VerifiableEmailAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'id1@example.com[CONFIRMED]'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 =  new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'id2@example.com[UNCONFIRMED]'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()}); 
		rules.add(new TranslationRule("true", action2));
		TranslationAction action3 =  new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'id3@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()}); 
		rules.add(new TranslationRule("true", action3));
		TranslationAction action4 = new TranslationAction(MapAttributeActionFactory.NAME, new String[] {
				"email", "/", "['id4@example.com[CONFIRMED]', 'id5@example.com[UNCONFIRMED]', 'id6@example.com']",
				AttributeEffectMode.CREATE_OR_UPDATE.toString()}); 
		rules.add(new TranslationRule("true", action4));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		
		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});
		
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
		
		
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/", "email");
		assertEquals(1, atrs.size());
		AttributeExt at = atrs.iterator().next();
		assertEquals(3, at.getValues().size());
		
		VerifiableEmail e1 = VerifiableEmail.fromJsonString(at.getValues().get(0));
		assertEquals("id4@example.com", e1.getValue());
		assertTrue(e1.isConfirmed());

		VerifiableEmail e2 = VerifiableEmail.fromJsonString(at.getValues().get(1));
		assertEquals("id5@example.com", e2.getValue());
		assertFalse(e2.isConfirmed());
		
		VerifiableEmail e3 = VerifiableEmail.fromJsonString(at.getValues().get(2));
		assertEquals("id6@example.com", e3.getValue());
		assertFalse(e3.isConfirmed());
	}

	
	
	@Test
	public void testIntegratedInputWithReg() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				X500Identity.ID, 
				"'CN=' + attr['cn'] + ',O=ICM,UID=' + id", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(MapGroupActionFactory.NAME, new String[] {
				"'/A'"}); 
		rules.add(new TranslationRule("true", action2));
		TranslationAction action3 = new TranslationAction(MapAttributeActionFactory.NAME, new String[] {
				"o", "/A", "groups",
				AttributeEffectMode.CREATE_OR_UPDATE.toString()}); 
		rules.add(new TranslationRule("true", action3));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);

		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("someUser", UsernameIdentity.ID));
		input.addAttribute(new RemoteAttribute("cn", "foo"));
		input.addGroup(new RemoteGroupMembership("mimuw"));
		input.addGroup(new RemoteGroupMembership("icm"));
		
		MappingResult result = tx.runInTransactionRetThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult resultIn = tp1.translate(input);
			inputTrEngine.process(resultIn);
			return resultIn;
		});
		
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
	public void testManualMergeWithExisting() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));

		MappingResult result = new MappingResult();
		result.addAttribute(new MappedAttribute(AttributeEffectMode.CREATE_ONLY, StringAttribute.of("o", 
				"/A", "org")));
		result.addGroup(new MappedGroup("/A", GroupEffectMode.ADD_IF_GROUP_EXISTS, "idp", "profile"));
		result.addIdentity(new MappedIdentity(IdentityEffectMode.CREATE_OR_MATCH, 
				new IdentityParam(UsernameIdentity.ID, "added"), "dummy"));
		
		setupPasswordAuthn();
		Identity baseUser = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam baseUserP = new EntityParam(baseUser);

		tx.runInTransactionThrowing(() -> {
			inputTrEngine.mergeWithExisting(result, baseUserP);
		});
		
		Entity entity = idsMan.getEntity(baseUserP);
		boolean hasBase = false;
		boolean hasNew = false;
		for (Identity id: entity.getIdentities())
		{
			if (id.getTypeId().equals(UsernameIdentity.ID))
			{
				if (DEF_USER.equals(id.getValue()))
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
		
		Collection<AttributeExt> atrs = attrsMan.getAttributes(baseUserP, "/A", "o");
		assertEquals(1, atrs.size());
		AttributeExt at = atrs.iterator().next();
		assertEquals(1, at.getValues().size());
		assertEquals("org", at.getValues().get(0).toString());
	}

	
	@Test
	public void primaryIdentityProperlySet() throws Exception
	{
		Identity toBeMappedOn = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "known"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				EntityState.valid, false);
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				IdentifierIdentity.ID, 
				"'unknown'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				IdentifierIdentity.ID, 
				"'known'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.MATCH.toString()});
		rules.add(new TranslationRule("true", action2));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);

		tprofMan.addProfile(tp1Cfg);
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		
		RemotelyAuthenticatedContext processed  = tx.runInTransactionRetThrowing(() -> {
			return remoteProcessor.processRemoteInput(input, "p1", false, Optional.empty());
		});
		
		assertNotNull(processed.getLocalMappedPrincipal());
		assertEquals(toBeMappedOn.getEntityId(), processed.getLocalMappedPrincipal().getEntityId().longValue());
	}
	
	@Test
	public void emailTagsArePreserved() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("email", VerifiableEmailAttributeSyntax.ID));
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'a+tag@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(MapAttributeActionFactory.NAME, new String[] {
				"email", 
				"/",
				"'b+tag@example.com'",
				AttributeEffectMode.CREATE_OR_UPDATE.toString()});
		rules.add(new TranslationRule("true", action2));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);

		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		//tprofMan.addProfile(tp1Cfg);

		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});

		
		EntityParam ep = new EntityParam(new IdentityTaV(EmailIdentity.ID, "a@example.com"));
		Entity entity = idsMan.getEntity(ep);
		boolean hasTagged = entity.getIdentities().stream().
				map(i -> i.getValue()).
				anyMatch(iv -> iv.equals("a+tag@example.com"));
		assertThat(hasTagged, is(true));
		
		Collection<AttributeExt> attrs = attrsMan.getAllAttributes(ep, false, "/", "email", true);
		assertThat(attrs.size(), is(1));
		List<String> values = attrs.iterator().next().getValues();
		assertThat(values.size(), is(1));
		assertThat((VerifiableEmail.fromJsonString(values.get(0))).getValue(), is("b+tag@example.com"));
	}
	
	
	@Test
	public void profileWithStaleActionsIsLoaded() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("someAttr", StringAttributeSyntax.ID));
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action = (TranslationAction) new TranslationAction(
				MapAttributeActionFactory.NAME, new String[] {"someAttr", "/", "'val'",  
						"CREATE_ONLY"}); 
		rules.add(new TranslationRule("true", action));
		
		TranslationProfile toAdd = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		tprofMan.addProfile(toAdd);
		
		Map<String, TranslationProfile> profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
		
		aTypeMan.removeAttributeType("someAttr", true);
		
		profiles = tprofMan.listInputProfiles();
		TranslationProfile retProfile = profiles.get("p1");
		assertNotNull(retProfile);
		assertEquals(1, retProfile.getRules().size());
		TranslationAction firstAction = retProfile.getRules().get(0).getAction();
		assertEquals(MapAttributeActionFactory.NAME, firstAction.getName());
		
		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile profileInstance = new InputTranslationProfile(retProfile, inputProfileRepo, intactionReg);
			TranslationActionInstance firstActionInstance = profileInstance.getRuleInstances().
					get(0).getActionInstance();
			assertThat(firstActionInstance, is(instanceOf(BlindStopperInputAction.class)));
		});
	}
	
	@Test
	public void shouldNotUpdateExistingConfirmedEmailAttributeNorIdentity() throws Exception
	{
		AttributeType oType = new AttributeType("email", VerifiableEmailAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		VerifiableEmail email = new VerifiableEmail("attr@example.com", new ConfirmationInfo(true));
		Attribute attr = VerifiableEmailAttribute.of("email", "/", email);
		attr.setRemoteIdp("remoteIDP_X");
		attr.setTranslationProfile("pX");
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "id@example.com");
		idParam.getConfirmationInfo().setConfirmed(true);
		
		Identity identity = idsMan.addEntity(idParam, 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false, Lists.newArrayList(attr));
		EntityParam ep = new EntityParam(identity);
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction actionId = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'id@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.UPDATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", actionId));
		TranslationAction actionAttr = new TranslationAction(MapAttributeActionFactory.NAME, new String[] {
				"email", "/", "['attr@example.com']",
				AttributeEffectMode.CREATE_OR_UPDATE.toString()}); 
		rules.add(new TranslationRule("true", actionAttr));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.INPUT, rules);
		
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("remoteIDP_Y");
		
		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});
		
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/", "email");
		assertEquals(1, atrs.size());
		AttributeExt at = atrs.iterator().next();
		assertEquals(1, at.getValues().size());
		
		VerifiableEmail e1 = VerifiableEmail.fromJsonString(at.getValues().get(0));
		assertEquals("attr@example.com", e1.getValue());
		assertTrue(e1.isConfirmed());
		
		Entity entity = idsMan.getEntity(ep);
		Identity emailIdentity = entity.getIdentities().stream()
				.filter(e -> e.getTypeId().equals(EmailIdentity.ID)).findAny().get();
		assertThat(emailIdentity.getValue(), is("id@example.com"));
		assertThat(emailIdentity.getConfirmationInfo().isConfirmed(), is(true));
	}
	
	
	@Test
	public void shouldUpdateExistingConfirmedEmailAttributeWithDifferentValue() throws Exception
	{
		AttributeType oType = new AttributeType("email", VerifiableEmailAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		VerifiableEmail email = new VerifiableEmail("attr@example.com", new ConfirmationInfo(true));
		Attribute attr = VerifiableEmailAttribute.of("email", "/", email);
		attr.setRemoteIdp("remoteIDP_X");
		attr.setTranslationProfile("pX");
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "id@example.com");
		
		Identity identity = idsMan.addEntity(idParam, 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false, Lists.newArrayList(attr));
		EntityParam ep = new EntityParam(identity);
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction actionId = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'id@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.UPDATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", actionId));
		TranslationAction actionAttr = new TranslationAction(MapAttributeActionFactory.NAME, new String[] {
				"email", "/", "['attr_NEW@example.com']",
				AttributeEffectMode.CREATE_OR_UPDATE.toString()}); 
		rules.add(new TranslationRule("true", actionAttr));
		TranslationProfile tp1Cfg = new TranslationProfile("p_Y", "", ProfileType.INPUT, rules);
		
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("remoteIDP_Y");
		
		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});
		
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/", "email");
		assertEquals(1, atrs.size());
		AttributeExt at = atrs.iterator().next();
		assertEquals(1, at.getValues().size());
		assertThat(at.getRemoteIdp(), is("remoteIDP_Y"));
		assertThat(at.getTranslationProfile(), is("p_Y"));
		
		VerifiableEmail e1 = VerifiableEmail.fromJsonString(at.getValues().get(0));
		assertEquals("attr_NEW@example.com", e1.getValue());
		assertThat(e1.isConfirmed(), is(false));
	}
	
	@Test
	public void shouldOverwriteRuleFromIncludedProfile() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("test", StringAttributeSyntax.ID));

		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action = (TranslationAction) new TranslationAction(
				MapAttributeActionFactory.NAME,
				new String[] { "test", "/", "'val'", "CREATE_OR_UPDATE" });
		rules.add(new TranslationRule("true", action));
		TranslationProfile included = new TranslationProfile("included", "",
				ProfileType.INPUT, rules);
		tprofMan.addProfile(included);
		
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "id@example.com");
		Identity identity = idsMan.addEntity(idParam, 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false, Lists.newArrayList());
		EntityParam ep = new EntityParam(identity);
		
		rules = new ArrayList<>();
		action = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'id@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.UPDATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action));
		
		action = (TranslationAction) new TranslationAction(
				IncludeInputProfileActionFactory.NAME, new String[] { "included" });
		rules.add(new TranslationRule("true", action));

		action = (TranslationAction) new TranslationAction(MapAttributeActionFactory.NAME,
				new String[] { "test", "/", "'val2'", "CREATE_OR_UPDATE" });
		rules.add(new TranslationRule("true", action));
		
		TranslationProfile toAdd = new TranslationProfile("p1", "", ProfileType.INPUT,
				rules);
		tprofMan.addProfile(toAdd);
				
		Map<String, TranslationProfile> profiles = tprofMan.listInputProfiles();
		assertNotNull(profiles.get("p1"));
					
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		
		tx.runInTransactionThrowing(() -> {
			InputTranslationProfile tp1 = new InputTranslationProfile(toAdd, inputProfileRepo, intactionReg);
			MappingResult result = tp1.translate(input);
			inputTrEngine.process(result);
		});
		
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/", "test");
		assertEquals(1, atrs.size());
		assertEquals("val2", atrs.iterator().next().getValues().get(0));	
	}
	
	@Test
	public void profileShouldNotFailIfAttributeIsMissing() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("test", StringAttributeSyntax.ID));
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action = (TranslationAction) new TranslationAction(
				MapAttributeActionFactory.NAME,
				new String[] { "test", "/", "'val'", "CREATE_OR_UPDATE" });
		rules.add(new TranslationRule("true", action));
		
		action = new TranslationAction(MapIdentityActionFactory.NAME, new String[] {
				EmailIdentity.ID, 
				"'id@example.com'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.UPDATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action));
		
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "id@example.com");
		Identity identity = idsMan.addEntity(idParam, 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false, Lists.newArrayList());
		EntityParam ep = new EntityParam(identity);
		
		TranslationProfile tp1Cfg = new TranslationProfile("tp1", "",
				ProfileType.INPUT, rules);
		tprofMan.addProfile(tp1Cfg);
	
		aTypeMan.removeAttributeType("test", true);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		try
		{
			tx.runInTransactionThrowing(() -> {
				InputTranslationProfile tp1 = new InputTranslationProfile(tp1Cfg,
						inputProfileRepo, intactionReg);
				MappingResult result = tp1.translate(input);
				inputTrEngine.process(result);
			});
		} catch (Exception e)
		{
			fail("Exception throw when run misconfigured action");
		}
	
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/", "test");
		assertEquals(0, atrs.size());
	}
	

}




