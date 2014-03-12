/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalTypeException;

public class TestTargetedPersistentIdentity
{
	@Test
	public void test() throws IllegalTypeException
	{
		TargetedPersistentIdentity tested = new TargetedPersistentIdentity(Constants.MAPPER);
		
		tested.resetIdentity(null, null, null);
		tested.resetIdentity("", "", null);
		tested.resetIdentity("", null, null);
		tested.resetIdentity(null, "", null);
		
		String inDb = tested.createNewIdentity("r1", "t1", null);
		Assert.assertNotNull(tested.toExternalForm("r1", "t1", inDb));
		Assert.assertNull(tested.toExternalForm("r2", "t1", inDb));
		Assert.assertNull(tested.toExternalForm("r1", "t2", inDb));
		
		String inDb2 = tested.createNewIdentity("r2", "t1", inDb);
		String inDb3 = tested.createNewIdentity("r2", "t2", inDb2);
		
		Assert.assertNotNull(tested.toExternalForm("r1", "t1", inDb3));
		Assert.assertNotNull(tested.toExternalForm("r2", "t1", inDb3));
		Assert.assertNotNull(tested.toExternalForm("r2", "t2", inDb3));
		
		String inDb4 = tested.resetIdentity("r2", "t2", inDb3);
		Assert.assertNotNull(tested.toExternalForm("r1", "t1", inDb4));
		Assert.assertNotNull(tested.toExternalForm("r2", "t1", inDb4));
		Assert.assertNull(tested.toExternalForm("r2", "t2", inDb4));
		
		String inDb5 = tested.resetIdentity(null, "t1", inDb3);
		Assert.assertNull(tested.toExternalForm("r1", "t1", inDb5));
		Assert.assertNull(tested.toExternalForm("r2", "t1", inDb5));
		Assert.assertNotNull(tested.toExternalForm("r2", "t2", inDb5));

		String inDb6 = tested.resetIdentity("r2", null, inDb3);
		Assert.assertNotNull(tested.toExternalForm("r1", "t1", inDb6));
		Assert.assertNull(tested.toExternalForm("r2", "t1", inDb6));
		Assert.assertNull(tested.toExternalForm("r2", "t2", inDb6));
	}
}
