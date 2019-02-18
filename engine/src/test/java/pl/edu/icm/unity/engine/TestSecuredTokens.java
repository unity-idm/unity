/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestSecuredTokens extends DBIntegrationTestBase
{
	@Autowired
	private SecuredTokensManagement securedTokensMan;

	@Autowired
	protected TokensManagement tokensMan;

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
		attrsMan.createAttribute(new EntityParam(id),
				EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
						InternalAuthorizationManagerImpl.USER_ROLE));
		attrsMan.createAttribute(new EntityParam(id2),
				EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
						InternalAuthorizationManagerImpl.USER_ROLE));

	}

	private void addTokens() throws Exception
	{
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));
		EntityParam ep2 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u2"));
		byte[] c = new byte[] { 'a' };
		Date exp = new Date(System.currentTimeMillis() + 500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);
		tokensMan.addToken("t2", "1234", ep1, c, new Date(), exp);
		tokensMan.addToken("t", "12345", ep1, c, new Date(), exp);
		tokensMan.addToken("t", "123456", ep2, c, new Date(), exp);
	}

	@Test
	public void shouldReturnTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();
		setupAdmin();
		Collection<Token> admTokens = securedTokensMan.getAllTokens("t");

		assertThat(admTokens.size(), is(3));
	}

	@Test
	public void shouldReturnOnlyOwnedTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();

		setupUserContext("u1", null);
		Collection<Token> u1Tokens = securedTokensMan.getAllTokens("t");

		assertThat(u1Tokens.size(), is(2));
	}

	@Test
	public void shouldForbidGetNotOwnedTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));

		setupUserContext("u2", null);
		catchException(securedTokensMan).getOwnedTokens("t", ep1);

		assertThat(caughtException(), isA(AuthorizationException.class));
	}

	@Test
	public void shouldReturnOwnedTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));

		setupUserContext("u1", null);
		Collection<Token> u1Tokens = securedTokensMan.getOwnedTokens("t", ep1);

		assertThat(u1Tokens.size(), is(2));
	}

	@Test
	public void shouldReturnOnlyOwnedToken() throws Exception
	{
		addRegularUsers();
		addTokens();

		setupUserContext("u1", null);
		Collection<Token> u1Tokens = securedTokensMan.getAllTokens(null);

		assertThat(u1Tokens.stream().filter(t -> t.getType().equals("t"))
				.collect(Collectors.toList()).size(), is(2));
		assertThat(u1Tokens.stream().filter(t -> t.getType().equals("t2"))
				.collect(Collectors.toList()).size(), is(1));

	}

	@Test
	public void shouldForbidGetNotOwnedToken() throws Exception
	{
		addRegularUsers();
		addTokens();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));

		setupUserContext("u2", null);
		catchException(securedTokensMan).getOwnedTokens(null, ep1);

		assertThat(caughtException(), isA(AuthorizationException.class));
	}

	@Test
	public void shouldRemoveTokenByOwner() throws Exception
	{
		addRegularUsers();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));
		byte[] c = new byte[] { 'a' };
		Date exp = new Date(System.currentTimeMillis() + 500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);

		setupUserContext("u1", null);
		securedTokensMan.removeToken("t", "1234");

		setupAdmin();

		assertThat(securedTokensMan.getAllTokens("t").size(), is(0));
	}

	@Test
	public void shouldForbidToRemoveNotOwnedToken() throws Exception
	{
		addRegularUsers();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));
		byte[] c = new byte[] { 'a' };
		Date exp = new Date(System.currentTimeMillis() + 500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);

		setupUserContext("u2", null);

		catchException(securedTokensMan).removeToken("t", "1234");
		assertThat(caughtException(), isA(AuthorizationException.class));

		setupAdmin();

		assertThat(securedTokensMan.getAllTokens("t").size(), is(1));
	}
}
