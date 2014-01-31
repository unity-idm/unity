/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationCondition;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.tactions.CreateUserActionFactory;
import pl.edu.icm.unity.stdext.tactions.MapAttributeToIdentityActionFactory;
import pl.edu.icm.unity.stdext.tactions.MapIdentityActionFactory;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
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
	
	
	@Test
	public void testPersistence() throws Exception
	{
		assertEquals(0, tprofMan.listProfiles().size());
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = tactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				"(.*)", "\\1", EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		rules.add(new TranslationRule(action1, new TranslationCondition()));
		
		TranslationAction action2 = tactionReg.getByName(CreateUserActionFactory.NAME).getInstance(
				"true");
		rules.add(new TranslationRule(action2, new TranslationCondition()));
		
		TranslationProfile toAdd = new TranslationProfile("p1", rules);
		tprofMan.addProfile(toAdd);
		
		Map<String, TranslationProfile> profiles = tprofMan.listProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(CreateUserActionFactory.NAME, profiles.get("p1").getRules().get(1).getAction().getName());
		assertEquals("true", profiles.get("p1").getRules().get(1).getAction().getParameters()[0]);
		
		rules.remove(0);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(CreateUserActionFactory.NAME, profiles.get("p1").getRules().get(0).getAction().getName());
		assertEquals("true", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		tprofMan.removeProfile("p1");
		assertEquals(0, tprofMan.listProfiles().size());
	}

	
	@Test
	public void testIntegrated() throws Exception
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = tactionReg.getByName(MapIdentityActionFactory.NAME).getInstance(
				"(.*)", "$1", EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		rules.add(new TranslationRule(action1, new TranslationCondition(
				"identities[\"cn=test\"] != null")));

		TranslationAction action2 = tactionReg.getByName(MapAttributeToIdentityActionFactory.NAME).getInstance(
				"uid", UsernameIdentity.ID, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		rules.add(new TranslationRule(action2, new TranslationCondition()));
		
		TranslationAction action3 = tactionReg.getByName(CreateUserActionFactory.NAME).getInstance(
				"true");
		rules.add(new TranslationRule(action3, new TranslationCondition()));
		
		TranslationProfile tp1 = new TranslationProfile("p1", rules);
		
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("cn=test", X500Identity.ID));
		input.addAttribute(new RemoteAttribute("uid", "foo"));
		
		tp1.translate(input);
		
		Entity entity = idsMan.getEntity(new EntityParam(new IdentityTaV(X500Identity.ID, "cn=test")));
		assertEquals(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				entity.getCredentialInfo().getCredentialRequirementId());
		assertEquals(3, entity.getIdentities().length);
	}
}



















