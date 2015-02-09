/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;

public class TestTargetedPersistentIdentity
{
	@Test
	public void test() throws IllegalTypeException, IllegalIdentityValueException
	{
		TargetedPersistentIdentity tested = new TargetedPersistentIdentity();
		
		IdentityRepresentation inDb = tested.createNewIdentity("r1", "t1", null);
		Assert.assertNotNull(tested.toExternalForm("r1", "t1", inDb.getContents()));
		try
		{
			tested.toExternalForm("r2", "t1", inDb.getContents());
		} catch (IllegalIdentityValueException e)
		{
			//OK
		}
		try
		{
			tested.toExternalForm("r1", "t2", inDb.getContents());
		} catch (IllegalIdentityValueException e)
		{
			//OK
		}
		
		Assert.assertEquals(inDb.getContents(), tested.toExternalForm("r1", "t1", inDb.getContents()));
	}
}
