/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.translation.out.action.FilterIdentityActionFactory;
import pl.edu.icm.unity.server.translation.out.action.FilterIdentityActionFactory.FilterIdentityAction;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

public class TestOutputIdentityActions
{
	@Test
	public void testFilterIdentityAll() throws EngineException
	{
		FilterIdentityActionFactory factory = new FilterIdentityActionFactory();
		
		FilterIdentityAction filterAllAction = factory.getInstance(null, null);
		
		Entity entity = createEntity(); 
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), entity, 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		filterAllAction.invoke(input, mvelCtx, "prof1", result);

		Collection<IdentityParam> identities = result.getIdentities();
		assertEquals(0, identities.size());
	}

	@Test
	public void testFilterIdentityByType() throws EngineException
	{
		FilterIdentityActionFactory factory = new FilterIdentityActionFactory();
		
		FilterIdentityAction filterByTypeAction = factory.getInstance(UsernameIdentity.ID, null);
		
		Entity entity = createEntity(); 
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), entity, 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		filterByTypeAction.invoke(input, mvelCtx, "prof1", result);

		Collection<IdentityParam> identities = result.getIdentities();
		assertEquals(2, identities.size());
		assertTrue(!findIdentity("user1", UsernameIdentity.ID, identities));
		assertTrue(findIdentity("CN=id2,O=ICM,C=PL", X500Identity.ID, identities));
		assertTrue(findIdentity("CN=u1,O=UW,C=PL", X500Identity.ID, identities));
	}

	@Test
	public void testFilterIdentityByValue() throws EngineException
	{
		FilterIdentityActionFactory factory = new FilterIdentityActionFactory();
		
		FilterIdentityAction filterByValueAction = factory.getInstance(null, ".*O=ICM.*");
		
		Entity entity = createEntity(); 
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), entity, 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		filterByValueAction.invoke(input, mvelCtx, "prof1", result);

		Collection<IdentityParam> identities = result.getIdentities();
		assertEquals(2, identities.size());
		assertTrue(findIdentity("user1", UsernameIdentity.ID, identities));
		assertTrue(!findIdentity("CN=id2,O=ICM,C=PL", X500Identity.ID, identities));
		assertTrue(findIdentity("CN=u1,O=UW,C=PL", X500Identity.ID, identities));
	}

	@Test
	public void testFilterIdentityByTypeValue() throws EngineException
	{
		FilterIdentityActionFactory factory = new FilterIdentityActionFactory();
		
		FilterIdentityAction filterByTypeValueAction = factory.getInstance(X500Identity.ID, ".*O=ICM.*");
		
		Entity entity = createEntity(); 
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), entity, 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		filterByTypeValueAction.invoke(input, mvelCtx, "prof1", result);

		Collection<IdentityParam> identities = result.getIdentities();
		assertEquals(2, identities.size());
		assertTrue(findIdentity("user1", UsernameIdentity.ID, identities));
		assertTrue(!findIdentity("CN=id2,O=ICM,C=PL", X500Identity.ID, identities));
		assertTrue(findIdentity("CN=u1,O=UW,C=PL", X500Identity.ID, identities));
	}

	public static Entity createEntity()
	{
		return new Entity(1L, new Identity[] {
				createIdentity("user1", new UsernameIdentity()),
				createIdentity("CN=id2,O=ICM,C=PL", new X500Identity()),
				createIdentity("CN=u1,O=UW,C=PL", new X500Identity())}, 
				EntityState.valid, null);
	}
	
	private boolean findIdentity(String name, String type, Collection<IdentityParam> identities)
	{
		for (IdentityParam id: identities)
		{
			if (id.getTypeId().equals(type) && id.getValue().equals(name))
				return true;
		}
		return false;
	}
	
	private static Identity createIdentity(String name, IdentityTypeDefinition idTypeDef)
	{
		Identity user = new Identity();
		user.setValue(name);
		IdentityType idT = new IdentityType(idTypeDef);
		user.setType(idT);
		user.setTypeId(idT.getIdentityTypeProvider().getId());
		return user;
	}
}
