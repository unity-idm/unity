/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class TestTokens extends DBIntegrationTestBase
{
	@Autowired
	protected TokensManagement tokensMan;
	
	@Test
	public void addedTokenIsReturnedById() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam ep = new EntityParam(id);
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);
		
		Token token = tokensMan.getTokenById("t", "1234");
		
		assertEquals("t", token.getType());
		assertEquals("1234", token.getValue());
		assertEquals(id.getEntityId(), token.getOwner().longValue());
		assertEquals('a', token.getContents()[0]);
		assertNotNull(token.getCreated());
		assertEquals(exp, token.getExpires());
	}
	
	@Test
	public void allOwnedTokensAreReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam ep = new EntityParam(id);
		byte[] c = new byte[] {'a'};
		tokensMan.addToken("t", "123", ep, c, new Date(), new Date(System.currentTimeMillis()+1000));
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);
		
		List<Token> tokens = tokensMan.getOwnedTokens("t", ep);	
		assertEquals(2, tokens.size());
	}

	@Test
	public void expiredTokenIsNotReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam ep = new EntityParam(id);
		byte[] c = new byte[] {'a'};
		tokensMan.addToken("t", "123", ep, c, new Date(), new Date(System.currentTimeMillis()+1));
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);

		Thread.sleep(1002);
		List<Token> tokens = tokensMan.getOwnedTokens("t", ep);

		assertEquals(1, tokens.size());
		assertThat(tokens.get(0).getValue(), is("1234"));
	}

	@Test
	public void updatedTokenIsReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam ep = new EntityParam(id);
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);
		
		tokensMan.updateToken("t", "1234", null, new byte[] {'b'});
		
		Token token = tokensMan.getTokenById("t", "1234");
		assertEquals("t", token.getType());
		assertEquals("1234", token.getValue());
		assertEquals(id.getEntityId(), token.getOwner().longValue());
		assertEquals('b', token.getContents()[0]);
		assertNotNull(token.getCreated());
		assertEquals(exp, token.getExpires());
	}

	@Test
	public void removedTokenIsNotReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam ep = new EntityParam(id);
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);
		
		tokensMan.removeToken("t", "1234");
		
		Throwable error = catchThrowable(() -> tokensMan.getTokenById("t", "1234"));	
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void stressTokenUpdates() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam ep = new EntityParam(id);
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);
		
		AtomicInteger counter = new AtomicInteger();
		int TRIES = 1000;
		Runnable updater = () -> 
		{
			for (int i=0; i<TRIES; i++)
			{
				tokensMan.updateToken("t", "1234", new Date(System.currentTimeMillis()+500000), c);
				counter.incrementAndGet();
			}
		};

		List<Thread> threads = new ArrayList<>();
		int THREADS = 2;
		for (int i=0; i<THREADS; i++)
			threads.add(new Thread(updater));
		for (int i=0; i<THREADS; i++)
			threads.get(i).start();
		for (int i=0; i<THREADS; i++)
			threads.get(i).join();
		
		assertThat(counter.get()).isEqualTo(THREADS * TRIES);
	}

	
}
