/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestTokens extends DBIntegrationTestBase
{
	@Autowired
	protected TokensManagement tokensMan;
	
	@Test
	public void addedTokenIsReturnedById() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
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
		assertEquals(TimeUtil.roundToS(exp), TimeUtil.roundToS(token.getExpires()));
	}
	
	@Test
	public void allOwnedTokensAreReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
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
				EntityState.valid, false);
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
				EntityState.valid, false);
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
		assertEquals(TimeUtil.roundToS(exp), TimeUtil.roundToS(token.getExpires()));
	}

	@Test
	public void removedTokenIsNotReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		EntityParam ep = new EntityParam(id);
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);
		
		tokensMan.removeToken("t", "1234");
		
		catchException(tokensMan).getTokenById("t", "1234");
		
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
}
