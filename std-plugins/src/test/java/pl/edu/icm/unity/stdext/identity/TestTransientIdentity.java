/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Date;

import org.junit.Test;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.Identity;

public class TestTransientIdentity
{
	@Test
	public void allFieldsAreSet() throws IllegalTypeException, IllegalIdentityValueException
	{
		TransientIdentity tested = new TransientIdentity();
		InvocationContext ctx = new InvocationContext(null, null, Collections.emptyList());
		InvocationContext.setCurrent(ctx);
		LoginSession ls = new LoginSession("1", new Date(), new Date(System.currentTimeMillis()+1000), 
				50, 1, "r1", null, null, null);
		ctx.setLoginSession(ls);
		
		Identity generated = tested.createNewIdentity("r1", "t1", 123l);
		
		assertThat(generated.getComparableValue(), is(notNullValue()));
		assertThat(generated.getValue(), is(notNullValue()));
		assertThat(generated.getMetadata(), is(notNullValue()));
		assertThat(generated.getRealm(), is("r1"));
		assertThat(generated.getTarget(), is("t1"));
		assertThat(generated.getEntityId(), is(123l));
	}
	
	@Test
	public void twoSessionsIdIsInCmpValue() throws IllegalTypeException, InterruptedException, IllegalIdentityValueException
	{
		TransientIdentity tested = new TransientIdentity();
		
		InvocationContext ctx = new InvocationContext(null, null, Collections.emptyList());
		InvocationContext.setCurrent(ctx);
		LoginSession ls = new LoginSession("SESS_ID", new Date(), new Date(System.currentTimeMillis()+1000), 
				50, 1, "r1", null, null, null);
		ctx.setLoginSession(ls);
		
		Identity generated = tested.createNewIdentity("r1", "t1", 123l);

		assertThat(generated.getComparableValue(), containsString("SESS_ID"));
	}
}
