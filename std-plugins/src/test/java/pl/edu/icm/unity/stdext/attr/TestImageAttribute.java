/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Paths;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.attr.UnityImage;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;

public class TestImageAttribute
{
	@Test
	public void test() throws Exception
	{
		ImageAttributeSyntax ias = new ImageAttributeSyntax();
		ias.getConfig().setMaxHeight(100);
		ias.getConfig().setMaxWidth(100);
		ias.getConfig().setMaxSize(20000);
		
		UnityImage bi = new UnityImage(Paths.get("src/test/resources/img/test-image_100x100.jpg"));
		ias.validate(bi);

		ias.getConfig().setMaxSize(100);
		try
		{
			ias.validate(bi);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		ias.getConfig().setMaxSize(20000);
		ias.getConfig().setMaxHeight(15);
		try
		{
			ias.validate(bi);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		ias.getConfig().setMaxHeight(16);
		ias.getConfig().setMaxWidth(15);
		try
		{
			ias.validate(bi);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		
		String s = ias.convertToString(bi);
		UnityImage after = ias.convertFromString(s);
		assertEquals(bi.getWidth(), after.getWidth());
		assertEquals(bi.getHeight(), after.getHeight());
		
		
		JsonNode cfg = ias.getSerializedConfiguration();
		ImageAttributeSyntax ias2 = new ImageAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		assertEquals(ias2.getConfig().getMaxWidth(), 15);
		assertEquals(ias2.getConfig().getMaxHeight(), 16);
		assertEquals(ias2.getMaxSize(), 20000);
	}
}
