/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.translation.out.CreateAttributeActionFactory;
import pl.edu.icm.unity.stdext.translation.out.CreateIdentityActionFactory;
import pl.edu.icm.unity.stdext.translation.out.CreatePersistentAttributeActionFactory;
import pl.edu.icm.unity.stdext.translation.out.CreatePersistentIdentityActionFactory;
import pl.edu.icm.unity.stdext.translation.out.CreateAttributeActionFactory.CreateAttributeAction;
import pl.edu.icm.unity.stdext.translation.out.CreateIdentityActionFactory.CreateIdentityAction;
import pl.edu.icm.unity.stdext.translation.out.CreatePersistentAttributeActionFactory.CreatePersistentAttributeAction;
import pl.edu.icm.unity.stdext.translation.out.CreatePersistentIdentityActionFactory.CreatePersistentIdentityAction;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

public class TestOutputCreateActions
{
	@Test
	public void testCreateAttribute() throws EngineException
	{
		CreateAttributeActionFactory factory = new CreateAttributeActionFactory();
		
		CreateAttributeAction createAction = factory.getInstance("a1", "'v1'","false");
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), 
				TestOutputIdentityActions.createEntity(), 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		createAction.invoke(input, mvelCtx, "prof1", result);

		Collection<DynamicAttribute> attributes = result.getAttributes();
		assertEquals(1, attributes.size());
		assertEquals(0, result.getAttributesToPersist().size());
		
		Iterator<DynamicAttribute> it = attributes.iterator();
		Attribute<?> a = it.next().getAttribute();
		assertEquals("a1", a.getName());
		assertEquals(1, a.getValues().size());
		assertEquals("v1", a.getValues().get(0));
	}
	
	
	@Test
	public void testCreatePersistedAttribute() throws EngineException
	{
		AttributesManagement attrsMan = mock(AttributesManagement.class);
		
		Map<String, AttributeType> mockAts = new HashMap<String, AttributeType>();
		AttributeType sA = new AttributeType("a1", new StringAttributeSyntax());
		mockAts.put(sA.getName(), sA);
		when(attrsMan.getAttributeTypesAsMap()).thenReturn(mockAts);
		
		CreatePersistentAttributeActionFactory factory = new CreatePersistentAttributeActionFactory(attrsMan);
		
		CreatePersistentAttributeAction createAction = factory.getInstance("a1", "'v1'","false","/A");
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), 
				TestOutputIdentityActions.createEntity(), 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		createAction.invoke(input, mvelCtx, "prof1", result);

		Collection<DynamicAttribute> attributes = result.getAttributes();
		assertEquals(1, attributes.size());
		assertEquals(1, result.getAttributesToPersist().size());
		assertEquals(result.getAttributesToPersist().iterator().next(), attributes.iterator().next().getAttribute());
		
		Attribute<?> a = attributes.iterator().next().getAttribute();
		assertEquals("a1", a.getName());
		assertEquals(1, a.getValues().size());
		assertEquals("v1", a.getValues().get(0));
		assertEquals("prof1", a.getTranslationProfile());
		assertEquals("/A", a.getGroupPath());
	}
	
	@Test
	public void testCreateIdentity() throws EngineException
	{
		CreateIdentityActionFactory factory = new CreateIdentityActionFactory();
		
		CreateIdentityAction createAction = factory.getInstance("i1", "'v1'");
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), 
				new Entity(1L, new Identity[] {}, EntityState.valid, null), 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		createAction.invoke(input, mvelCtx, "prof1", result);

		Collection<IdentityParam> identities = result.getIdentities();
		assertEquals(1, identities.size());
		assertEquals(0, result.getIdentitiesToPersist().size());
		
		Iterator<IdentityParam> it = identities.iterator();
		IdentityParam id = it.next();
		assertEquals("i1", id.getTypeId());
		assertEquals("v1", id.getValue());
	}

	@Test
	public void testCreatePersistedIdentity() throws EngineException
	{
		IdentityTypesRegistry idReg = mock(IdentityTypesRegistry.class);
		IdentityTypeDefinition mockIdType = mock(IdentityTypeDefinition.class);
		when(mockIdType.isDynamic()).thenReturn(false);
		when(mockIdType.getId()).thenReturn("i1");
		when(mockIdType.convertFromString("v1", null, "prof1")).thenReturn(new IdentityParam("i1", "v1"));
		when(idReg.getByName("i1")).thenReturn(mockIdType);
		
		CreatePersistentIdentityActionFactory factory = new CreatePersistentIdentityActionFactory(idReg);
		CreatePersistentIdentityAction createAction = factory.getInstance("i1", "'v1'");
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute<?>>(), 
				new Entity(1L, new Identity[] {}, EntityState.valid, null), 
				"/", new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		createAction.invoke(input, mvelCtx, "prof1", result);

		Collection<IdentityParam> identities = result.getIdentities();
		assertEquals(1, identities.size());
		assertEquals(1, result.getIdentitiesToPersist().size());
		assertEquals(identities.iterator().next(), result.getIdentitiesToPersist().iterator().next());
		
		Iterator<IdentityParam> it = identities.iterator();
		IdentityParam id = it.next();
		assertEquals("i1", id.getTypeId());
		assertEquals("v1", id.getValue());
	}

}
