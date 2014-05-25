/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.stdext.utils.Escaper;

public class TestEscaper
{
	@Test
	public void test()
	{
		String a = Escaper.encode("aa", "bb", "cc");
		String[] sp = Escaper.decode(a);
		Assert.assertEquals(3, sp.length);
		Assert.assertEquals("aa", sp[0]);
		Assert.assertEquals("bb", sp[1]);
		Assert.assertEquals("cc", sp[2]);

		a = Escaper.encode("$a$", "\\b", "cc\\$");
		sp = Escaper.decode(a);
		Assert.assertEquals(3, sp.length);
		Assert.assertEquals("$a$", sp[0]);
		Assert.assertEquals("\\b", sp[1]);
		Assert.assertEquals("cc\\$", sp[2]);
	}
}
