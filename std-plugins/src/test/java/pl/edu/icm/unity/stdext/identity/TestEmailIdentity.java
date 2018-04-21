/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;

public class TestEmailIdentity
{
	@Test
	public void testInvalid() throws IllegalTypeException, InterruptedException, IllegalIdentityValueException
	{
		checkIfInvalid("local");
		checkIfInvalid("test@@ex.com");
		checkIfInvalid("test@ex..com");
		checkIfInvalid("+test@example.com");
		checkIfInvalid("test<@example.com");
		checkIfInvalid("test@examplecom");
		checkIfInvalid("@example.com");
	}

	@Test
	public void testValid() throws IllegalTypeException, InterruptedException, IllegalIdentityValueException
	{
		checkIfValid("email@e.pl");
		checkIfValid("email+tag+tag2@long.long.example.com");
	}
	
	private void checkIfInvalid(String toCheck) throws IllegalIdentityValueException
	{
		EmailIdentity emailId = new EmailIdentity();
		catchException(emailId).validate(toCheck);
		assertThat(caughtException(), instanceOf(IllegalIdentityValueException.class));
	}

	private void checkIfValid(String toCheck) throws IllegalIdentityValueException
	{
		EmailIdentity emailId = new EmailIdentity();
		emailId.validate(toCheck);
	}
	
	@Test
	public void comparisonDoesNotDependOnCase() throws Exception
	{
		EmailIdentity idType = new EmailIdentity();
		String id1 = idType.getComparableValue("UppeR@example.com", null, null);
		String id2 = idType.getComparableValue("uPPer@examplE.com", null, null);
		
		assertThat(id1, is(id2));
	}
}
