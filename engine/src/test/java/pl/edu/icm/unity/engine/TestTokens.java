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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestTokens extends DBIntegrationTestBase
{
	@Autowired
	protected TokensManagement tokensMan;
	
	@Autowired SecuredTokensManagement securedTokensMan;
	
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
		assertEquals(exp, token.getExpires());
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
		assertEquals(exp, token.getExpires());
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

	private void addRegularUsers() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");

		Identity id = idsMan.addEntity(toAdd,
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				EntityState.valid, false);
		toAdd.setValue("u2");
		Identity id2 = idsMan.addEntity(toAdd,
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				EntityState.valid, false);
		attrsMan.setAttribute(new EntityParam(id),
				EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
						AuthorizationManagerImpl.USER_ROLE),
				false);
		attrsMan.setAttribute(new EntityParam(id2),
				EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
						AuthorizationManagerImpl.USER_ROLE),
				false);

	}
	
	@Test
	public void secureListToken() throws Exception
	{
		addRegularUsers();	
		EntityParam ep1 = new EntityParam( new IdentityParam(UsernameIdentity.ID, "u1"));
		EntityParam ep2 = new EntityParam( new IdentityParam(UsernameIdentity.ID, "u2"));
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);
		tokensMan.addToken("t", "12345", ep1, c, new Date(), exp);
		tokensMan.addToken("t", "123456", ep2, c, new Date(), exp);
		
		setupAdmin();
		Collection<Token> admTokens = securedTokensMan.getAllTokens("t");
		assertEquals(3, admTokens.size());
		
		setupUserContext("u1", false);
		Collection<Token> u1Tokens = securedTokensMan.getAllTokens("t");
		assertEquals(2, u1Tokens.size());
		
		setupUserContext("u2", false);
		Collection<Token> u2Tokens = securedTokensMan.getAllTokens("t");
		assertEquals(1, u2Tokens.size());
		
		setupUserContext("u2", false);
		catchException(securedTokensMan).getOwnedTokens("t", ep1);
		assertThat(caughtException(), isA(AuthorizationException.class));
		
		setupUserContext("u1", false);
		u1Tokens = securedTokensMan.getOwnedTokens("t", ep1);
		assertEquals(2, u1Tokens.size());
		
	}
	
	@Test
	public void secureRemoveTokenByOwner() throws Exception
	{
		addRegularUsers();
		EntityParam ep1 = new EntityParam( new IdentityParam(UsernameIdentity.ID, "u1"));
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);
	
		setupUserContext("u1", false);
		securedTokensMan.removeToken("t", "1234");
		
		setupAdmin();
		assertEquals(0, securedTokensMan.getAllTokens("t").size());
	}
	
	@Test
	public void secureRemoveTokenByNotOwner() throws Exception
	{
		addRegularUsers();
		EntityParam ep1 = new EntityParam( new IdentityParam(UsernameIdentity.ID, "u1"));
		byte[] c = new byte[] {'a'};
		Date exp = new Date(System.currentTimeMillis()+500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);
	
		setupUserContext("u2", false);
		catchException(securedTokensMan).removeToken("t", "1234");
		assertThat(caughtException(), isA(AuthorizationException.class));
		
		setupAdmin();
		assertEquals(1, securedTokensMan.getAllTokens("t").size());
	}
	
}
