/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Same as {@link SecuredDBIntegrationTestBase} but additionally puts admin user in authentication context
 * so all operations are authZed 
 * @author K. Benedyczak
 */
public abstract class DBIntegrationTestBase extends SecuredDBIntegrationTestBase
{
	@Autowired
	private SessionManagement sessionMan;
	
	@Before
	public void setupAdmin() throws Exception
	{
		setupUserContext("admin", false);
	}
	
	@After
	public void clearAuthnCtx() throws EngineException
	{
		InvocationContext.setCurrent(null);
	}	
	
	protected void setupUserContext(String user, boolean outdated) throws Exception
	{
		EntityWithCredential entity = identityResolver.resolveIdentity(user, new String[] {UsernameIdentity.ID}, 
				EngineInitialization.DEFAULT_CREDENTIAL);
		InvocationContext virtualAdmin = new InvocationContext();
		LoginSession ls = sessionMan.getCreateSession(entity.getEntityId(), getDefaultRealm(),
				user, outdated, null);
		virtualAdmin.setLoginSession(ls);
		virtualAdmin.setLocale(Locale.ENGLISH);
		//override for tests: it can happen that existing session is returned, therefore old state of cred is
		// there.
		ls.setUsedOutdatedCredential(outdated);
		InvocationContext.setCurrent(virtualAdmin);
	}
	
	private AuthenticationRealm getDefaultRealm()
	{
		return new AuthenticationRealm("DEFAULT_AUTHN_REALM", 
				"For tests", 5, 10, -1, 30*60);
	}
}
