/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

/**
 * Tests {@link IntegerAttributeSyntax}
 * @author K. Benedyczak
 */
public class TestNumericAttribute
{
	@Test
	public void testInteger() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		ias.setMax(12);
		ias.setMin(-33);

		ias.validate(12L);
		ias.validate(-33L);
		
		try
		{
			ias.validate(13L);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}

		try
		{
			ias.validate(-34L);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		
		long before = 123123123123L;
		String s = ias.convertToString(before);
		long after = ias.convertFromString(s);
		assertEquals(before, after);
		
		assertTrue(ias.areEqual(1234L, new Long(1234)));
		assertFalse(ias.areEqual(1235L, new Long(1234)));
		
		JsonNode cfg = ias.getSerializedConfiguration();
		
		IntegerAttributeSyntax ias2 = new IntegerAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		assertEquals(ias2.getMax(), 12);
		assertEquals(ias2.getMin(), -33);
	}

	@Test
	public void testFloating() throws Exception
	{
		FloatingPointAttributeSyntax ias = new FloatingPointAttributeSyntax();
		ias.setMax(12.5);
		ias.setMin(-33.88);

		ias.validate(12.5);
		ias.validate(-33.88);
		
		try
		{
			ias.validate(12.6);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}

		try
		{
			ias.validate(-33.89);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		
		double before = 123123123123L;
		String s = ias.convertToString(before);
		double after = ias.convertFromString(s);
		assertEquals(before, after, 0.01);
		
		assertTrue(ias.areEqual(1234.3, new Double(1234.3)));
		assertFalse(ias.areEqual(1235.4, new Double(1234.3)));
		
		JsonNode cfg = ias.getSerializedConfiguration();
		
		FloatingPointAttributeSyntax ias2 = new FloatingPointAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		assertEquals(ias2.getMax(), 12.5, 0);
		assertEquals(ias2.getMin(), -33.88, 0);
	}
}
