/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.authz;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestAuthorizationMan extends DBIntegrationTestBase
{
	@Autowired
	private AuthorizationManagement authzMan;

	@Test
	public void shouldHaveAdminAccess() throws AuthorizationException
	{
		assertThat(authzMan.hasAdminAccess(), is(true));
	}
	
	@Test
	public void shouldNotHaveAdminAccess() throws Exception
	{
		setupPasswordAuthn();
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS,
				EntityState.valid, false);
		setupUserContext("tuser", null);
		assertThat(authzMan.hasAdminAccess(), is(false));
	}

}
