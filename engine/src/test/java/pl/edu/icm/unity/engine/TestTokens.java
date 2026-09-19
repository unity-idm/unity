/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class TestTokens extends DBIntegrationTestBase
{
	@Autowired
	protected TokensManagement tokensMan;
	@Autowired
	protected TransactionalRunner tx;
	
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
		
		assertThat(token.getType()).isEqualTo("t");
		assertThat(token.getValue()).isEqualTo("1234");
		assertThat(token.getOwner().longValue()).isEqualTo(id.getEntityId());
		assertThat(token.getContents()).isEqualTo(new byte[] {'a'});
		assertThat(token.getCreated()).isNotNull();
		assertThat(token.getExpires()).isEqualTo(exp);
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
		assertThat(tokens).hasSize(2);
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

		assertThat(tokens).hasSize(1);
		assertThat(tokens.get(0).getValue()).isEqualTo("1234");
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
		assertThat(token.getType()).isEqualTo("t");
		assertThat(token.getValue()).isEqualTo("1234");
		assertThat(token.getOwner().longValue()).isEqualTo(id.getEntityId());
		assertThat(token.getContents()).isEqualTo(new byte[] {'b'});
		assertThat(token.getCreated()).isNotNull();
		assertThat(token.getExpires()).isEqualTo(exp);
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

	@Test
	public void getTokenByIdForUpdateSerializesConcurrentReadModifyWrite() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				EntityState.valid);
		EntityParam ep = new EntityParam(id);
		Date exp = new Date(System.currentTimeMillis() + 500000);
		tokensMan.addToken("lockt", "1234", ep, new byte[] { 0 }, new Date(), exp);

		int THREADS = 4;
		int INCREMENTS_PER_THREAD = 25;
		Runnable incrementer = () ->
		{
			for (int i = 0; i < INCREMENTS_PER_THREAD; i++)
			{
				tx.runInTransaction(() ->
				{
					Token current = tokensMan.getTokenByIdForUpdate("lockt", "1234");
					int value = current.getContents()[0] & 0xFF;
					// widens the race window: without the row lock, two threads reading
					// the same value here would both write back value+1, losing an update
					try
					{
						Thread.sleep(1);
					} catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
					tokensMan.updateToken("lockt", "1234", null, new byte[] { (byte) (value + 1) });
				});
			}
		};

		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < THREADS; i++)
			threads.add(new Thread(incrementer));
		for (Thread t : threads)
			t.start();
		for (Thread t : threads)
			t.join();

		Token result = tokensMan.getTokenById("lockt", "1234");
		assertThat(result.getContents()[0] & 0xFF).isEqualTo(THREADS * INCREMENTS_PER_THREAD);
	}
}
