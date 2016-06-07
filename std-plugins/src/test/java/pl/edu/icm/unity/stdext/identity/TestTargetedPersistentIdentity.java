/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.Identity;

public class TestTargetedPersistentIdentity
{
	@Test
	public void allFieldsAreSet() throws IllegalTypeException, IllegalIdentityValueException
	{
		TargetedPersistentIdentity tested = new TargetedPersistentIdentity();
		
		Identity generated = tested.createNewIdentity("r1", "t1", 123l);
		
		assertThat(generated.getComparableValue(), is(notNullValue()));
		assertThat(generated.getValue(), is(notNullValue()));
		assertThat(generated.getRealm(), is("r1"));
		assertThat(generated.getTarget(), is("t1"));
		assertThat(generated.getEntityId(), is(123l));
	}
}
