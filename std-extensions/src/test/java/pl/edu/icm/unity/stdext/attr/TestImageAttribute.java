/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;

import javax.imageio.ImageIO;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

public class TestImageAttribute
{
	@Test
	public void test() throws Exception
	{
		JpegImageAttributeSyntax ias = new JpegImageAttributeSyntax();
		ias.setMaxHeight(16);
		ias.setMaxWidth(16);
		ias.setMaxSize(20000);
		
		BufferedImage bi = ImageIO.read(new FileInputStream("src/test/resources/img/add.png"));
		ias.validate(bi);

		ias.setMaxSize(100);
		try
		{
			ias.validate(bi);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		ias.setMaxSize(20000);
		ias.setMaxHeight(15);
		try
		{
			ias.validate(bi);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		ias.setMaxHeight(16);
		ias.setMaxWidth(15);
		try
		{
			ias.validate(bi);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		
		byte[] s = ias.serialize(bi);
		BufferedImage after = ias.deserialize(s);
		assertEquals(bi.getWidth(), after.getWidth());
		assertEquals(bi.getHeight(), after.getHeight());
		
		
		String cfg = ias.getSerializedConfiguration();
		JpegImageAttributeSyntax ias2 = new JpegImageAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		assertEquals(ias2.getMaxWidth(), 15);
		assertEquals(ias2.getMaxHeight(), 16);
		assertEquals(ias2.getMaxSize(), 20000);
	}
}
