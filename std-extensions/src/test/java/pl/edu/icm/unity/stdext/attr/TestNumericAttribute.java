/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static org.junit.Assert.*;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

/**
 * Tests {@link IntegerAttributeSyntax}
 * @author K. Benedyczak
 */
public class TestNumericAttribute
{
	@Test
	public void testInteger()
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
		byte[] s = ias.serialize(before);
		long after = ias.deserialize(s);
		assertEquals(before, after);
		
		assertTrue(ias.areEqual(1234L, new Long(1234)));
		assertFalse(ias.areEqual(1235L, new Long(1234)));
		
		String cfg = ias.getSerializedConfiguration();
		
		IntegerAttributeSyntax ias2 = new IntegerAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		assertEquals(ias2.getMax(), 12);
		assertEquals(ias2.getMin(), -33);
	}

	@Test
	public void testFloating()
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
		byte[] s = ias.serialize(before);
		double after = ias.deserialize(s);
		assertEquals(before, after, 0.01);
		
		assertTrue(ias.areEqual(1234.3, new Double(1234.3)));
		assertFalse(ias.areEqual(1235.4, new Double(1234.3)));
		
		String cfg = ias.getSerializedConfiguration();
		
		FloatingPointAttributeSyntax ias2 = new FloatingPointAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		assertEquals(ias2.getMax(), 12.5, 0);
		assertEquals(ias2.getMin(), -33.88, 0);
	}
}
