/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.AttributeType;
import pl.edu.icm.unity.types.AttributeVisibility;

public class TestAttributes extends DBIntegrationTestBase
{
	@Test
	public void testSyntaxes() throws Exception
	{
		String[] supportedSyntaxes = attrsMan.getSupportedAttributeValueTypes();
		Arrays.sort(supportedSyntaxes);
		assertEquals(1, supportedSyntaxes.length);
		assertEquals(StringAttributeSyntax.ID, supportedSyntaxes[0]);
	
	}

	@Test
	public void testCreate() throws Exception
	{
		List<AttributeType> ats = attrsMan.getAttributeTypes();
		assertEquals(0, ats.size());

		AttributeType at = new AttributeType();
		at.setValueType(new StringAttributeSyntax());
		at.setDescription("desc");
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName("some");
		at.setSelfModificable(true);
		at.setVisibility(AttributeVisibility.local);
		attrsMan.addAttributeType(at);
		
		ats = attrsMan.getAttributeTypes();
		assertEquals(1, ats.size());
		AttributeType at2 = ats.get(0);
		
		assertEquals(at.getDescription(), at2.getDescription());
		assertEquals(at.getFlags(), at2.getFlags());
		assertEquals(at.getMaxElements(), at2.getMaxElements());
		assertEquals(at.getMinElements(), at2.getMinElements());
		assertEquals(at.getName(), at2.getName());
		assertEquals(at.getValueType().getValueSyntaxId(), at2.getValueType().getValueSyntaxId());
		assertEquals(at.getVisibility(), at2.getVisibility());
		
		at.setName("some2");
		try
		{
			at.setFlags(100);
			attrsMan.addAttributeType(at);
			fail("Managed to add attr with flags");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		at.setFlags(0);

		try
		{
			at.setMaxElements(100);
			at.setMinElements(200);
			attrsMan.addAttributeType(at);
			fail("Managed to add attr with wrong min/max");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		at.setMinElements(0);

		try
		{
			at.setName("some");
			attrsMan.addAttributeType(at);
			fail("Managed to add attr with duplicated name");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		
	}
}
