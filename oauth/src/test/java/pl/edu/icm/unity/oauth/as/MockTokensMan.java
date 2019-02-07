/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.EntityParam;

public class MockTokensMan implements TokensManagement
{
	private Map<String, Token> tokens = new HashMap<>();
		
	
	@Override
	public void addToken(String type, String value, EntityParam owner, byte[] contents,
			Date created, Date expires) throws 
			IllegalIdentityValueException, IllegalTypeException
	{
		long entityId = owner.getEntityId() == null ? owner.getIdentity().hashCode() : owner.getEntityId();
		Token t = new Token(type, value, entityId);
		t.setContents(contents);
		t.setExpires(expires);
		t.setCreated(created);
		tokens.put(type+value, t);
	}

	@Override
	public void removeToken(String type, String value) 
	{
		tokens.remove(type+value);
	}

	@Override
	public void updateToken(String type, String value, Date expires, byte[] contents)
	{
		Token t = getTokenById(type, value);
		t.setContents(contents);
		t.setExpires(expires);
		tokens.put(type+value, t);
	}

	@Override
	public Token getTokenById(String type, String value) 
	{
		if (!tokens.containsKey(type+value))
			throw new IllegalArgumentException("no such token");
		return tokens.get(type+value);
	}

	@Override
	public List<Token> getOwnedTokens(String type, EntityParam entity)
			throws IllegalIdentityValueException, IllegalTypeException
	{
		throw new RuntimeException("unimplemented");
	}

	@Override
	public List<Token> getAllTokens(String type)
	{
		return new ArrayList<>(tokens.values());
	}

	@Override
	public void addTokenExpirationListener(TokenExpirationListener listener, String type)
	{
		throw new RuntimeException("unimplemented");
	}

	@Override
	public void addToken(String type, String value, byte[] contents, Date created, Date expires)
			throws IllegalTypeException
	{
	}

	@Override
	public List<Token> getAllTokens()
	{
		return null;
	}
}