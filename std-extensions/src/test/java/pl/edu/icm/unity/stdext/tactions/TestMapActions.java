/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AttributeEffectMode;
import pl.edu.icm.unity.server.authn.remote.translation.IdentityEffectMode;
import pl.edu.icm.unity.server.authn.remote.translation.MappedIdentity;
import pl.edu.icm.unity.server.authn.remote.translation.MappingResult;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;

public class TestMapActions
{
	@Test
	public void testMapAttribute() throws EngineException
	{
		MapAttributeActionFactory factory = new MapAttributeActionFactory(new MockAttributesMan());
		
		TranslationAction mapAction = factory.getInstance("stringA", "/A/B", 
				"attr['attribute'] + '-' + attr['other'] + '-' + id", 
				AttributeVisibility.full.toString(), AttributeEffectMode.CREATE_OR_UPDATE.toString());
				
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("idd", "i1"));
		input.addAttribute(new RemoteAttribute("attribute", "a1"));
		input.addAttribute(new RemoteAttribute("other", "a2"));
		
		MappingResult result = mapAction.invoke(input, TranslationProfile.createMvelContext(input), "testProf");
		
		Attribute<?> a = result.getAttributes().get(0).getAttribute();
		assertEquals("stringA", a.getName());
		assertEquals("a1-a2-idd", a.getValues().get(0));
	}

	@Test
	public void testMapGroup() throws EngineException
	{
		MapGroupActionFactory factory = new MapGroupActionFactory();
		TranslationAction mapAction = factory.getInstance("'/A/B/' + attr['attribute']");
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addAttribute(new RemoteAttribute("attribute", "a1"));
		
		MappingResult result = mapAction.invoke(input, TranslationProfile.createMvelContext(input), "testProf");
		
		assertTrue(result.getGroups().contains("/A/B/a1"));
	}
	
	@Test
	public void testMapIdentity() throws EngineException
	{
		MapIdentityActionFactory factory = new MapIdentityActionFactory();
		TranslationAction mapAction = factory.getInstance("userName", 
				"attr['attribute:colon'] + '-' + attr['other'] + '-' + id", 
				"CR", IdentityEffectMode.REQUIRE_MATCH.toString());
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("idvalue", "idtype"));
		input.addAttribute(new RemoteAttribute("attribute:colon", "a1"));
		input.addAttribute(new RemoteAttribute("other", "a2"));
		
		MappingResult result = mapAction.invoke(input, TranslationProfile.createMvelContext(input), "testProf");
		
		MappedIdentity mi = result.getIdentities().get(0);
		assertEquals("CR", mi.getCredentialRequirement());
		assertEquals(IdentityEffectMode.REQUIRE_MATCH, mi.getMode());
		assertEquals("userName", mi.getIdentity().getTypeId());
		assertEquals("a1-a2-idvalue", mi.getIdentity().getValue());
	}
	
	
	
	
	private static class MockAttributesMan implements AttributesManagement
	{
		@Override
		public String[] getSupportedAttributeValueTypes() throws EngineException
		{
			return null;
		}

		@Override
		public void addAttributeType(AttributeType at) throws EngineException
		{
		}

		@Override
		public void updateAttributeType(AttributeType at) throws EngineException
		{
		}

		@Override
		public void removeAttributeType(String id, boolean deleteInstances)
				throws EngineException
		{
		}

		@Override
		public Collection<AttributeType> getAttributeTypes() throws EngineException
		{
			return null;
		}

		@Override
		public Map<String, AttributeType> getAttributeTypesAsMap() throws EngineException
		{
			Map<String, AttributeType> ret = new HashMap<String, AttributeType>();
			AttributeType sA = new AttributeType("stringA", new StringAttributeSyntax());
			ret.put(sA.getName(), sA);
			return ret;
		}

		@Override
		public void addAttributeClass(AttributesClass clazz) throws EngineException
		{
		}
		@Override
		public void removeAttributeClass(String id) throws EngineException
		{
		}
		@Override
		public void updateAttributeClass(AttributesClass updated) throws EngineException
		{
		}
		@Override
		public Map<String, AttributesClass> getAttributeClasses() throws EngineException
		{
			return null;
		}
		@Override
		public void setEntityAttributeClasses(EntityParam entity, String group,
				Collection<String> classes) throws EngineException
		{
		}
		@Override
		public Collection<AttributesClass> getEntityAttributeClasses(EntityParam entity,
				String group) throws EngineException
		{
			return null;
		}
		@Override
		public <T> void setAttribute(EntityParam entity, Attribute<T> attribute,
				boolean update) throws EngineException
		{
		}
		@Override
		public void removeAttribute(EntityParam entity, String groupPath,
				String attributeTypeId) throws EngineException
		{
		}
		@Override
		public Collection<AttributeExt<?>> getAttributes(EntityParam entity,
				String groupPath, String attributeTypeId) throws EngineException
		{
			return null;
		}
		@Override
		public Collection<AttributeExt<?>> getAllAttributes(EntityParam entity,
				boolean effective, String groupPath, String attributeTypeId,
				boolean allowDegrade) throws EngineException
		{
			return null;
		}
	}
}
