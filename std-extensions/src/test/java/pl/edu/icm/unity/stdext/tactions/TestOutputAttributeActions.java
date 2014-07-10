/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.tactions.out.FilterAttributeAction;
import pl.edu.icm.unity.stdext.tactions.out.FilterAttributeActionFactory;
import pl.edu.icm.unity.stdext.tactions.out.FilterAttributeValuesAction;
import pl.edu.icm.unity.stdext.tactions.out.FilterAttributeValuesActionFactory;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

public class TestOutputAttributeActions
{
	@Test
	public void testFilterAttribute() throws EngineException
	{
		FilterAttributeActionFactory factory = new FilterAttributeActionFactory();
		
		FilterAttributeAction filterAllAction = factory.getInstance("a1");
		
		StringAttribute a1 = new StringAttribute("a1", "/", AttributeVisibility.full);
		StringAttribute a2 = new StringAttribute("a2", "/", AttributeVisibility.full);
		TranslationInput input = new TranslationInput(Arrays.asList(a1, a2), 
				TestOutputIdentityActions.createEntity(), 
				new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		filterAllAction.invoke(input, mvelCtx, "prof1", result);

		Collection<Attribute<?>> attributes = result.getAttributes();
		assertEquals(1, attributes.size());
		assertEquals("a2", attributes.iterator().next().getName());
	}

	@Test
	public void testFilterAttributeValues() throws EngineException
	{
		FilterAttributeValuesActionFactory factory = new FilterAttributeValuesActionFactory();
		
		FilterAttributeValuesAction filterAllAction = factory.getInstance("a1", "v[23].*");
		
		StringAttribute a1 = new StringAttribute("a1", "/", AttributeVisibility.full, "v1", "v2", "v3");
		StringAttribute a2 = new StringAttribute("a2", "/", AttributeVisibility.full, "vv1", "vv2");
		TranslationInput input = new TranslationInput(Arrays.asList(a1, a2), 
				TestOutputIdentityActions.createEntity(),
				new HashSet<String>(), "RE1", "P1", "SP1");
		
		Object mvelCtx = OutputTranslationProfile.createMvelContext(input);
		TranslationResult result = OutputTranslationProfile.initiateTranslationResult(input);
		filterAllAction.invoke(input, mvelCtx, "prof1", result);

		Collection<Attribute<?>> attributes = result.getAttributes();
		assertEquals(2, attributes.size());
		Map<String, Attribute<?>> attributesMap = new HashMap<>();
		Iterator<Attribute<?>> it = attributes.iterator();
		Attribute<?> a = it.next();
		attributesMap.put(a.getName(), a);
		a = it.next();
		attributesMap.put(a.getName(), a);
		
		assertNotNull(attributesMap.get("a1"));
		assertEquals(1, attributesMap.get("a1").getValues().size());
		assertNotNull(attributesMap.get("a2"));
		assertEquals(2, attributesMap.get("a2").getValues().size());
	}
}
