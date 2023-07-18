/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.authz;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

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
		assertThat(authzMan.hasAdminAccess()).isTrue();
	}
	
	@Test
	public void shouldNotHaveAdminAccess() throws Exception
	{
		setupPasswordAuthn();
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS,
				EntityState.valid);
		setupUserContext("tuser", null);
		assertThat(authzMan.hasAdminAccess()).isFalse();
	}

}
