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

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.types.basic.EntityParam;

public class MockTokensMan implements TokensManagement
{
	private Map<String, Token> tokens = new HashMap<>();
		
	
	@Override
	public Object startTokenTransaction()
	{
		return null;
	}

	@Override
	public void commitTokenTransaction(Object transaction)
	{
	}

	@Override
	public void closeTokenTransaction(Object transaction)
	{
	}

	@Override
	public void addToken(String type, String value, EntityParam owner, byte[] contents,
			Date created, Date expires, Object transaction)
			throws WrongArgumentException, IllegalIdentityValueException,
			IllegalTypeException
	{
		addToken(type, value, owner, contents, created, expires);
	}

	@Override
	public void addToken(String type, String value, EntityParam owner, byte[] contents,
			Date created, Date expires) throws WrongArgumentException,
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
	public void removeToken(String type, String value, Object transaction)
			throws WrongArgumentException
	{
		removeToken(type, value);
	}

	@Override
	public void removeToken(String type, String value) throws WrongArgumentException
	{
		tokens.remove(type+value);
	}

	@Override
	public void updateToken(String type, String value, Date expires, byte[] contents,
			Object transaction) throws WrongArgumentException
	{
		updateToken(type, value, expires, contents);
	}

	@Override
	public void updateToken(String type, String value, Date expires, byte[] contents)
			throws WrongArgumentException
	{
		Token t = getTokenById(type, value);
		t.setContents(contents);
		t.setExpires(expires);
		tokens.put(type+value, t);
	}

	@Override
	public Token getTokenById(String type, String value, Object transaction)
			throws WrongArgumentException
	{
		return getTokenById(type, value);
	}

	@Override
	public Token getTokenById(String type, String value) throws WrongArgumentException
	{
		if (!tokens.containsKey(type+value))
			throw new WrongArgumentException("no such token");
		return tokens.get(type+value);
	}

	@Override
	public List<Token> getOwnedTokens(String type, EntityParam entity,
			Object transaction) throws IllegalIdentityValueException,
			IllegalTypeException
	{
		return getOwnedTokens(type, entity);
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
	public void addToken(String type, String value, byte[] contents, Date created,
			Date expires, Object transaction) throws WrongArgumentException,
			IllegalTypeException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addToken(String type, String value, byte[] contents, Date created, Date expires)
			throws WrongArgumentException, IllegalTypeException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Token> getAllTokens(String type, Object transaction)
	{
		return getAllTokens(type, null);
	}
}