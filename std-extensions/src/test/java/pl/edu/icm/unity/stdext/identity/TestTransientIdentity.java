/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;

public class TestTransientIdentity
{
	@Test
	public void test() throws IllegalTypeException, InterruptedException, IllegalIdentityValueException
	{
		TransientIdentity tested = new TransientIdentity(Constants.MAPPER);
		
		InvocationContext ctx = new InvocationContext(null, null);
		InvocationContext.setCurrent(ctx);
		
		LoginSession ls = new LoginSession("1", new Date(), new Date(System.currentTimeMillis()+1000), 
				50, 1, "r1");
		ctx.setLoginSession(ls);
		
		IdentityRepresentation inDb = tested.createNewIdentity("r1", "t1", null);
		tested.toExternalForm("r1", "t1", inDb.getContents());
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
		
		IdentityRepresentation inDb2 = tested.createNewIdentity("r1", "t1", "test");
		Assert.assertEquals("test",
				tested.toExternalForm("r1", "t1", inDb2.getContents()));
		
		LoginSession ls2 = new LoginSession("2", new Date(), 30, 1, "r1");
		ctx.setLoginSession(ls2);
		
		IdentityRepresentation inDb21 = tested.createNewIdentity("r1", "t1", "test");
		Assert.assertNotEquals(inDb2.getComparableValue(), inDb21.getComparableValue());

		ctx.setLoginSession(ls);
		
		IdentityRepresentation inDb22 = tested.createNewIdentity("r1", "t1", "test");
		Assert.assertEquals(inDb2.getComparableValue(), inDb22.getComparableValue());
	}
}
