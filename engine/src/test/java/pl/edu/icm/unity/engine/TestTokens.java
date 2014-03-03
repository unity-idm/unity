/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestTokens extends DBIntegrationTestBase
{
	@Autowired
	protected TokensManagement tokensMan;
	
	@Test
	public void test() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1", true);
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		EntityParam ep = new EntityParam(id);

		byte[] c = new byte[] {'a'};
		
		tokensMan.addToken("t", "123", ep, c, new Date(), new Date(System.currentTimeMillis()+100));
		Date exp = new Date(System.currentTimeMillis()+100000);
		tokensMan.addToken("t", "1234", ep, c, new Date(), exp);
		List<Token> tokens = tokensMan.getOwnedTokens("t", ep);
		assertEquals(2, tokens.size());
		Thread.sleep(101);
		tokens = tokensMan.getOwnedTokens("t", ep);
		assertEquals(1, tokens.size());
		Token token = tokensMan.getTokenById("t", "1234");
		assertEquals("t", token.getType());
		assertEquals("1234", token.getValue());
		assertEquals(id.getEntityId().longValue(), token.getOwner());
		assertEquals('a', token.getContents()[0]);
		assertNotNull(token.getCreated());
		assertEquals(exp, token.getExpires());
		
		tokensMan.updateToken("t", "1234", null, new byte[] {'b'});
		token = tokensMan.getTokenById("t", "1234");
		assertEquals("t", token.getType());
		assertEquals("1234", token.getValue());
		assertEquals(id.getEntityId().longValue(), token.getOwner());
		assertEquals('b', token.getContents()[0]);
		assertNotNull(token.getCreated());
		assertEquals(exp, token.getExpires());
		
		tokensMan.removeToken("t", "1234");
		
		try
		{
			tokensMan.getTokenById("t", "1234");
		} catch (WrongArgumentException e)
		{
			//OK
		}
	}
}
