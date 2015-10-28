/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestTransactionRetry extends DBIntegrationTestBase
{
	public static final int LOOP = 1000;
	public static final int TOKENS = 100;
	
	@Autowired
	protected TokensManagement tokensMan;
	private boolean failure;
	
	@Test
	public void test() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		EntityParam ep = new EntityParam(id);

		byte[] c = new byte[] {'a'};

		for (int i=0; i<TOKENS; i++)
			tokensMan.addToken("t", String.valueOf(i), ep, c, new Date(), 
					new Date(System.currentTimeMillis()+1000000));

		Thread[] threads = new Thread[8];
		Random rand = new Random();
		failure = false;
		
		for (int i=0; i<4; i++)
		{
			threads[i] = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						for (int i=0; i<LOOP; i++)
							tokensMan.updateToken("t", String.valueOf(rand.nextInt(TOKENS)), 
								null, new byte[] {(byte) rand.nextInt(TOKENS)});
					} catch (Exception e)
					{
						e.printStackTrace();
						failure = true;
					}					
				}
			});
			threads[i].start();
		}
		for (int i=4; i<8; i++)
		{
			threads[i] = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						for (int i=0; i<LOOP; i++)
							tokensMan.getOwnedTokens("t", ep);
					} catch (Exception e)
					{
						e.printStackTrace();
						failure = true;
					}
				}
			});
			threads[i].start();
		}

		for (int i=0; i<8; i++)
			threads[i].join();
		
		if (failure)
			fail("Some threads failded, check logs");
	}
}
