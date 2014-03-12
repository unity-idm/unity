/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;

public class TestTransientIdentity
{
	@Test
	public void test() throws IllegalTypeException, InterruptedException
	{
		TransientIdentity tested = new TransientIdentity(Constants.MAPPER);
		
		tested.resetIdentity(null, null, null);
		tested.resetIdentity("", "", null);
		tested.resetIdentity("", null, null);
		tested.resetIdentity(null, "", null);
		InvocationContext ctx = new InvocationContext();
		InvocationContext.setCurrent(ctx);
		
		LoginSession ls = new LoginSession("1", new Date(), new Date(System.currentTimeMillis()+1000), 
				50, 1, "r1");
		ctx.setLoginSession(ls);
		
		String inDb = tested.createNewIdentity("r1", "t1", null);
		String ext1 = tested.toExternalForm("r1", "t1", inDb);
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
		
		LoginSession ls2 = new LoginSession("2", new Date(), 30, 1, "r1");
		ctx.setLoginSession(ls2);
		
		String inDb21 = tested.createNewIdentity("r1", "t1", inDb);
		String ext2 = tested.toExternalForm("r1", "t1", inDb21);
		Assert.assertNotEquals(ext2, ext1);

		ctx.setLoginSession(ls);
		Assert.assertEquals(tested.toExternalForm("r1", "t1", inDb21), ext1);
		
		//This test to work would need to remove the min 24h expiration
//		ctx.setLoginSession(ls2);
//		Thread.sleep(301);
//		String inDb22 = tested.createNewIdentity("r1", "t3", inDb21);
//		Assert.assertNull(tested.toExternalForm("r1", "t1", inDb22));
	}
}
