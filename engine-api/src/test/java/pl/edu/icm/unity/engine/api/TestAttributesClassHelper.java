/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;

public class TestAttributesClassHelper
{
	@Test
	public void testCleanup() throws Exception
	{
		AttributesClass ac1 = new AttributesClass("ac1", "", Collections.singleton("a1"), 
				Collections.singleton("m1"), false, new HashSet<String>());
		
		
		AttributesClass ac2 = new AttributesClass("ac2", "", Collections.singleton("a2"), 
				Collections.singleton("m2"), false, Collections.singleton("ac1"));
		AttributesClass ac3 = new AttributesClass("ac3", "", Collections.singleton("a3"), 
				Collections.singleton("m3"), false, Collections.singleton("ac2"));
		AttributesClass ac4 = new AttributesClass("ac4", "", Collections.singleton("a4"), 
				Collections.singleton("m4"), false, Collections.singleton("ac1"));
		
		AttributesClass tested = new AttributesClass("tested", "", 
				new HashSet<String>(Arrays.asList("a1", "a2", "a3", "a4", "a5")), 
				new HashSet<String>(Arrays.asList("m1", "m3", "m4")), 
				false, new HashSet<String>(Arrays.asList("ac3", "ac4", "ac1", "ac2")));
		Map<String, AttributesClass> knownClasses = new HashMap<>();
		knownClasses.put(ac1.getName(), ac1);
		knownClasses.put(ac2.getName(), ac2);
		knownClasses.put(ac3.getName(), ac3);
		knownClasses.put(ac4.getName(), ac4);
		
		AttributeClassHelper.cleanupClass(ac1, knownClasses);
		assertThat(ac1.isAllowArbitrary()).isFalse();
		
		AttributeClassHelper.cleanupClass(tested, knownClasses);
		
		assertThat(tested.getParentClasses()).hasSize(2);
		assertThat(tested.getParentClasses().contains("ac4")).isTrue();
		assertThat(tested.getParentClasses().contains("ac3")).isTrue();
		
		assertThat(tested.isAllowArbitrary()).isFalse();
		assertThat(tested.getAllowed()).hasSize(1);
		assertThat(tested.getAllowed().contains("a5")).isTrue();
		
		assertThat(tested.getMandatory()).hasSize(0);
		
		
		
		ac1.setAllowArbitrary(true);
		tested = new AttributesClass("tested", "", 
				new HashSet<String>(Arrays.asList("a1", "a2", "a3", "a4", "a5")), 
				new HashSet<String>(Arrays.asList("m1", "m3", "m4")), 
				false, new HashSet<String>(Arrays.asList("ac3", "ac4", "ac1", "ac2")));
		AttributeClassHelper.cleanupClass(tested, knownClasses);
		assertThat(tested.getAllowed()).hasSize(0);
		assertThat(tested.isAllowArbitrary()).isTrue();

	}
}
