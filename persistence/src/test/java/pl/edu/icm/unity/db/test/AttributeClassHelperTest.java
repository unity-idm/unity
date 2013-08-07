/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.test;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.types.basic.AttributesClass;


public class AttributeClassHelperTest
{
	@Test
	public void test() throws IllegalTypeException
	{
		AttributeClassHelper helper = new AttributeClassHelper();
		assertTrue(helper.isAllowed("foo"));
		assertFalse(helper.isMandatory("foo"));
		
		AttributesClass parent = new AttributesClass("parent", "", Collections.singleton("a"), 
				Collections.singleton("b"), false, new HashSet<String>(0));
		Set<String> allowed = new HashSet<>();
		Collections.addAll(allowed, "a", "c");
		AttributesClass child = new AttributesClass("child", "", allowed, 
				new HashSet<String>(), false, Collections.singleton(parent.getName()));
		
		Map<String, AttributesClass> allAc = new HashMap<String, AttributesClass>();
		allAc.put(parent.getName(), parent);
		allAc.put(child.getName(), child);
		helper = new AttributeClassHelper(allAc, Collections.singleton(child.getName()));
		
		assertTrue(helper.isAllowed("a"));
		assertTrue(helper.isAllowed("b"));
		assertTrue(helper.isAllowed("c"));
		assertFalse(helper.isAllowed("d"));

		assertFalse(helper.isMandatory("a"));
		assertTrue(helper.isMandatory("b"));
		assertFalse(helper.isMandatory("c"));
		assertFalse(helper.isMandatory("d"));

		
		AttributesClass another = new AttributesClass("another", "", Collections.singleton("z"), 
				Collections.singleton("y"), true, new HashSet<String>(0));
		allAc.put(another.getName(), another);
		Set<String> acs = new HashSet<>();
		Collections.addAll(acs, another.getName(), child.getName());
		helper = new AttributeClassHelper(allAc, acs);
		assertTrue(helper.isAllowed("a"));
		assertTrue(helper.isAllowed("b"));
		assertTrue(helper.isAllowed("c"));
		assertTrue(helper.isAllowed("d"));
		assertTrue(helper.isAllowed("z"));
		assertTrue(helper.isAllowed("y"));

		assertFalse(helper.isMandatory("a"));
		assertTrue(helper.isMandatory("b"));
		assertFalse(helper.isMandatory("c"));
		assertFalse(helper.isMandatory("d"));
		assertTrue(helper.isMandatory("y"));
		assertFalse(helper.isMandatory("z"));
		
		
		child = new AttributesClass("child", "", allowed, 
				new HashSet<String>(), true, Collections.singleton(parent.getName()));
		allAc.put(child.getName(), child);
		helper = new AttributeClassHelper(allAc, Collections.singleton(child.getName()));

		assertTrue(helper.isAllowed("a"));
		assertTrue(helper.isAllowed("b"));
		assertTrue(helper.isAllowed("c"));
		assertTrue(helper.isAllowed("d"));

		assertFalse(helper.isMandatory("a"));
		assertTrue(helper.isMandatory("b"));
		assertFalse(helper.isMandatory("c"));
		assertFalse(helper.isMandatory("d"));
		
		
		
	}
}
