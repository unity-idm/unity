/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;
import org.junit.Test;


import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

public class TestAttribute
{
	@Test
	public void testEquals()
	{
		Attribute<String> a1 = new Attribute<String>("a", new MockAttributeSyntax(), "/a", 
				AttributeVisibility.full, null);
		Attribute<String> a2 = new Attribute<String>("a2", new MockAttributeSyntax(), "/a", 
				AttributeVisibility.full, null);
		Attribute<String> a3 = new Attribute<String>("a", new MockAttributeSyntax(), "/a3", 
				AttributeVisibility.full, null);
		Attribute<String> a4 = new Attribute<String>("a", new MockAttributeSyntax(), "/a3", 
				AttributeVisibility.full, Collections.singletonList("a"));
		Attribute<String> a5 = new Attribute<String>("a", new MockAttributeSyntax(), "/a3", 
				AttributeVisibility.full, Collections.singletonList("a"));
		Attribute<String> a6 = new Attribute<String>("a", new MockAttributeSyntax(), "/a", 
				AttributeVisibility.full, new ArrayList<String>());
		
		assertFalse(a1.equals(a2));
		assertFalse(a1.equals(a3));
		assertFalse(a1.equals(a4));
		assertFalse(a1.equals(a5));
		assertTrue(a1.equals(a6));

		assertFalse(a2.equals(a1));
		assertFalse(a2.equals(a3));
		assertFalse(a2.equals(a4));
		assertFalse(a2.equals(a5));
		assertFalse(a2.equals(a6));

		assertFalse(a3.equals(a1));
		assertFalse(a3.equals(a2));
		assertFalse(a3.equals(a4));
		assertFalse(a3.equals(a5));
		assertFalse(a3.equals(a6));
		
		assertFalse(a4.equals(a1));
		assertFalse(a4.equals(a2));
		assertFalse(a4.equals(a3));
		assertTrue(a4.equals(a5));
		assertFalse(a4.equals(a6));

		assertFalse(a5.equals(a1));
		assertFalse(a5.equals(a2));
		assertFalse(a5.equals(a3));
		assertTrue(a5.equals(a4));
		assertFalse(a5.equals(a6));

		assertFalse(a1.hashCode() == a2.hashCode());
		assertFalse(a1.hashCode() == a3.hashCode());
		assertFalse(a1.hashCode() == a4.hashCode());
		assertFalse(a1.hashCode() == a5.hashCode());
		assertTrue(a1.hashCode() == a6.hashCode());

		assertFalse(a2.hashCode() == a3.hashCode());
		assertFalse(a2.hashCode() == a4.hashCode());
		assertFalse(a2.hashCode() == a5.hashCode());
		assertFalse(a2.hashCode() == a6.hashCode());

		assertFalse(a3.hashCode() == a4.hashCode());
		assertFalse(a3.hashCode() == a5.hashCode());
		assertFalse(a3.hashCode() == a6.hashCode());

		assertTrue(a4.hashCode() == a5.hashCode());
		assertFalse(a4.hashCode() == a6.hashCode());

		assertFalse(a5.hashCode() == a6.hashCode());
	}
}
