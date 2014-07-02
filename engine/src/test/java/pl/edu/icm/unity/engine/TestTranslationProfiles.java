/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
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
import pl.edu.icm.unity.server.authn.remote.TranslationEngine;
import pl.edu.icm.unity.server.authn.remote.translation.AttributeEffectMode;
import pl.edu.icm.unity.server.authn.remote.translation.IdentityEffectMode;
import pl.edu.icm.unity.server.authn.remote.translation.MappingResult;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationCondition;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile.ProfileMode;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.tactions.MapAttributeActionFactory;
import pl.edu.icm.unity.stdext.tactions.MapGroupActionFactory;
import pl.edu.icm.unity.stdext.tactions.MapIdentityActionFactory;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
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
	protected TranslationEngine trEngine;
	
	@Test
	public void testPersistence() throws Exception
	{
		assertEquals(0, tprofMan.listProfiles().size());
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = tactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				IdentifierIdentity.ID, 
				"'joe'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new TranslationRule(action1, new TranslationCondition()));
		TranslationAction action2 = tactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'"); 
		rules.add(new TranslationRule(action2, new TranslationCondition()));
		
		TranslationProfile toAdd = new TranslationProfile("p1", rules, ProfileMode.UPDATE_ONLY);
		tprofMan.addProfile(toAdd);
		
		Map<String, TranslationProfile> profiles = tprofMan.listProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(MapIdentityActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getActionDescription().getName());
		assertEquals(IdentifierIdentity.ID, profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		rules.remove(0);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(MapGroupActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getActionDescription().getName());
		assertEquals("'/A'", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		tprofMan.removeProfile("p1");
		assertEquals(0, tprofMan.listProfiles().size());
	}

	
	@Test
	public void testIntegrated() throws Exception
	{
		AttributeType oType = new AttributeType("o", new StringAttributeSyntax());
		oType.setMaxElements(10);
		attrsMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = tactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				X500Identity.ID, 
				"'CN=' + attr['cn'] + ',O=ICM,UID=' + id", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString());
		rules.add(new TranslationRule(action1, new TranslationCondition()));
		TranslationAction action2 = tactionReg.getByName(MapGroupActionFactory.NAME).getInstance(
				"'/A'"); 
		rules.add(new TranslationRule(action2, new TranslationCondition()));
		TranslationAction action3 = tactionReg.getByName(MapAttributeActionFactory.NAME).getInstance(
				"o", "/A", "groups",
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString()); 
		rules.add(new TranslationRule(action3, new TranslationCondition()));
		
		TranslationProfile tp1 = new TranslationProfile("p1", rules, ProfileMode.UPDATE_ONLY);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("someUser", UsernameIdentity.ID));
		input.addAttribute(new RemoteAttribute("cn", "foo"));
		input.addGroup(new RemoteGroupMembership("mimuw"));
		input.addGroup(new RemoteGroupMembership("icm"));
		
		MappingResult result = tp1.translate(input);
		trEngine.process(result);
		
		EntityParam ep = new EntityParam(new IdentityTaV(X500Identity.ID, "CN=foo,O=ICM,UID=someUser"));
		Entity entity = idsMan.getEntity(ep);
		assertEquals(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				entity.getCredentialInfo().getCredentialRequirementId());
		assertEquals(2, entity.getIdentities().length);
		Collection<AttributeExt<?>> atrs = attrsMan.getAttributes(ep, "/A", "o");
		assertEquals(1, atrs.size());
		Attribute<?> at = atrs.iterator().next();
		assertEquals(2, at.getValues().size());
		assertEquals("mimuw", at.getValues().get(0));
		assertEquals("icm", at.getValues().get(1));
	}
}



















