/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Same as {@link SecuredDBIntegrationTestBase} but additionally puts admin user in authentication context
 * so all operations are authZed 
 * @author K. Benedyczak
 */
public abstract class DBIntegrationTestBase extends SecuredDBIntegrationTestBase
{
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
		InvocationContext virtualAdmin = new InvocationContext(null);
		virtualAdmin.setAuthenticatedEntity(new AuthenticatedEntity(entity.getEntityId(), user, outdated));
		virtualAdmin.setLocale(Locale.ENGLISH);
		InvocationContext.setCurrent(virtualAdmin);
	}
}
