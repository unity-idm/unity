/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;

import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class TestRealms extends DBIntegrationTestBase
{
	@Test
	public void testRealmsMan() throws Exception
	{
		Collection<AuthenticationRealm> realms = realmsMan.getRealms();
		assertEquals(0, realms.size());
		
		AuthenticationRealm r = new AuthenticationRealm("some realm", "desc", 
				10, 11, RememberMePolicy.disallow , 2, 22);
		realmsMan.addRealm(r);
		
		AuthenticationRealm r2 = realmsMan.getRealm(r.getName());
		assertEquals(r.getName(), r2.getName());
		assertEquals(r.getDescription(), r2.getDescription());
		assertEquals(r.getAllowForRememberMeDays(), r2.getAllowForRememberMeDays());
		assertEquals(r.getBlockAfterUnsuccessfulLogins(), r2.getBlockAfterUnsuccessfulLogins());
		assertEquals(r.getBlockFor(), r2.getBlockFor());
		assertEquals(r.getMaxInactivity(), r2.getMaxInactivity());
		
		assertEquals(1, realmsMan.getRealms().size());
		
		r = new AuthenticationRealm("some realm", "desc2", 
				11, 12, RememberMePolicy.disallow , 3, 33);
		
		realmsMan.updateRealm(r);
		
		r2 = realmsMan.getRealm(r.getName());
		assertEquals(r.getName(), r2.getName());
		assertEquals(r.getDescription(), r2.getDescription());
		assertEquals(r.getAllowForRememberMeDays(), r2.getAllowForRememberMeDays());
		assertEquals(r.getBlockAfterUnsuccessfulLogins(), r2.getBlockAfterUnsuccessfulLogins());
		assertEquals(r.getBlockFor(), r2.getBlockFor());
		assertEquals(r.getMaxInactivity(), r2.getMaxInactivity());
		
		realmsMan.removeRealm(r.getName());
		
		assertEquals(0, realmsMan.getRealms().size());
	}
}



















