/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;

public class TestRealms extends DBIntegrationTestBase
{
	@Test
	public void testRealmsMan() throws Exception
	{
		Collection<AuthenticationRealm> realms = realmsMan.getRealms();
		assertThat(realms).isEmpty();
		
		AuthenticationRealm r = new AuthenticationRealm("some realm", "desc", 
				10, 11, RememberMePolicy.disallow , 2, 22);
		realmsMan.addRealm(r);
		
		AuthenticationRealm r2 = realmsMan.getRealm(r.getName());
		assertThat(r2.getName()).isEqualTo(r.getName());
		assertThat(r2.getDescription()).isEqualTo(r.getDescription());
		assertThat(r2.getAllowForRememberMeDays()).isEqualTo(r.getAllowForRememberMeDays());
		assertThat(r2.getBlockAfterUnsuccessfulLogins()).isEqualTo(r.getBlockAfterUnsuccessfulLogins());
		assertThat(r2.getBlockFor()).isEqualTo(r.getBlockFor());
		assertThat(r2.getMaxInactivity()).isEqualTo(r.getMaxInactivity());
		
		assertThat(realmsMan.getRealms()).hasSize(1);
		
		r = new AuthenticationRealm("some realm", "desc2", 
				11, 12, RememberMePolicy.disallow , 3, 33);
		
		realmsMan.updateRealm(r);
		
		r2 = realmsMan.getRealm(r.getName());
		assertThat(r2.getName()).isEqualTo(r.getName());
		assertThat(r2.getDescription()).isEqualTo(r.getDescription());
		assertThat(r2.getAllowForRememberMeDays()).isEqualTo(r.getAllowForRememberMeDays());
		assertThat(r2.getBlockAfterUnsuccessfulLogins()).isEqualTo(r.getBlockAfterUnsuccessfulLogins());
		assertThat(r2.getBlockFor()).isEqualTo(r.getBlockFor());
		assertThat(r2.getMaxInactivity()).isEqualTo(r.getMaxInactivity());
		
		realmsMan.removeRealm(r.getName());
		
		assertThat(realmsMan.getRealms()).isEmpty();
	}
}



















