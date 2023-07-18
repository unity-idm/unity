/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;

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
		
		assertThat(generated.getComparableValue()).isNotNull();
		assertThat(generated.getValue()).isNotNull();
		assertThat(generated.getMetadata()).isNotNull();
		assertThat(generated.getRealm()).isEqualTo("r1");
		assertThat(generated.getTarget()).isEqualTo("t1");
		assertThat(generated.getEntityId()).isEqualTo(123l);
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

		assertThat(generated.getComparableValue()).containsSequence("SESS_ID");
	}
}
