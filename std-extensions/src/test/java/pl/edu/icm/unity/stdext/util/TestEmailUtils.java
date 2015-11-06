/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.utils.EmailUtils;

public class TestEmailUtils
{
	@Test
	public void parsingWorks()
	{
		VerifiableEmail converted = EmailUtils.convertFromString("a@b.com[CONFIRMED]");
		Assert.assertTrue(converted.isConfirmed());
		Assert.assertEquals("a@b.com", converted.getValue());

		VerifiableEmail converted2 = EmailUtils.convertFromString("a@b.com[UNCONFIRMED]");
		Assert.assertFalse(converted2.isConfirmed());
		Assert.assertEquals("a@b.com", converted2.getValue());

		VerifiableEmail converted3 = EmailUtils.convertFromString("a@b.com");
		Assert.assertFalse(converted3.isConfirmed());
		Assert.assertEquals("a@b.com", converted3.getValue());
		
		VerifiableEmail converted4 = EmailUtils.convertFromString(" a@b.com   ");
		Assert.assertEquals("a@b.com", converted4.getValue());
	}
}
