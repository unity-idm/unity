/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.tx;

import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestTransactionRetry extends DBIntegrationTestBase
{
	public static final int LOOP = 200;
	public static final int THREADS = 8;
	
	@Autowired
	protected SessionManagement sessionMan;
	private boolean failure;
	
	@Test
	public void testTransactionRetry() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		Thread[] threads = new Thread[8];
		failure = false;
		Date expiration = new Date(System.currentTimeMillis() + 1000000);
		AuthenticationRealm realm = new AuthenticationRealm("realm", "desc", 10, 1000, 3, 100000);
		for (int i=0; i<THREADS; i++)
		{
			threads[i] = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						for (int i=0; i<LOOP; i++)
						{
							LoginSession createSession = sessionMan.getCreateSession(
									id.getEntityId(), 
									realm, "somelabel", false, expiration);
							sessionMan.updateSessionActivity(createSession.getId());
						}
					} catch (Exception e)
					{
						e.printStackTrace();
						failure = true;
					}					
				}
			});
			threads[i].start();
		}
		for (int i=0; i<THREADS; i++)
			threads[i].join();
		
		if (failure)
			fail("Some threads failded, check logs");
	}
}
