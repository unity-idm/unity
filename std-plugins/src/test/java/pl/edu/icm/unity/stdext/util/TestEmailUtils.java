/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.stdext.utils.EmailUtils;

public class TestEmailUtils
{
	@Test
	public void parsingWorks()
	{
		VerifiableEmail converted = EmailUtils.convertFromString("a@b.com[CONFIRMED]");
		assertThat(converted.isConfirmed()).isTrue();
		assertThat("a@b.com").isEqualTo(converted.getValue());

		VerifiableEmail converted2 = EmailUtils.convertFromString("a@b.com[UNCONFIRMED]");
		assertThat(converted2.isConfirmed()).isFalse();
		assertThat("a@b.com").isEqualTo(converted2.getValue());

		VerifiableEmail converted3 = EmailUtils.convertFromString("a@b.com");
		assertThat(converted3.isConfirmed()).isFalse();
		assertThat("a@b.com").isEqualTo(converted3.getValue());
		
		VerifiableEmail converted4 = EmailUtils.convertFromString(" a@b.com   ");
		assertThat("a@b.com").isEqualTo(converted4.getValue());
	}
	
	@Test
	public void emailWithTagIsEqualToNotTagged()
	{
		VerifiableEmail email = new VerifiableEmail("a+tag1@ex.com");
		
		assertThat(email).isEqualTo(new VerifiableEmail("a@ex.com "));
	}

	@Test
	public void emailWithoutTagIsEqualToTagged()
	{
		VerifiableEmail email = new VerifiableEmail("a@ex.com");
		
		assertThat(email).isEqualTo(new VerifiableEmail("a+tag@ex.com "));
	}

	@Test
	public void emailsWithTagsAreEqual()
	{
		VerifiableEmail email = new VerifiableEmail("a+tag1@ex.com");
		
		assertThat(email).isEqualTo(new VerifiableEmail(" a+tag1@ex.com "));
	}
	
	@Test
	public void shouldValidateEmailWithNewTLD()
	{
		String status = EmailUtils.validate("some@some.inc");
		
		assertThat(status).isNull();
	}
}
