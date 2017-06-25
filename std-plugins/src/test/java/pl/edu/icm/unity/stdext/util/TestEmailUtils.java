/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

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
	
	@Test
	public void emailWithTagIsEqualToNotTagged()
	{
		VerifiableEmail email = new VerifiableEmail("a+tag1@ex.com");
		
		assertThat(email, equalTo(new VerifiableEmail("a@ex.com ")));
	}

	@Test
	public void emailWithoutTagIsEqualToTagged()
	{
		VerifiableEmail email = new VerifiableEmail("a@ex.com");
		
		assertThat(email, equalTo(new VerifiableEmail("a+tag@ex.com ")));
	}

	@Test
	public void emailsWithTagsAreEqual()
	{
		VerifiableEmail email = new VerifiableEmail("a+tag1@ex.com");
		
		assertThat(email, equalTo(new VerifiableEmail(" a+tag1@ex.com ")));
	}
}
