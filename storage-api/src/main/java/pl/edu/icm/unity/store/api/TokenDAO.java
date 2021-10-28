/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.base.token.Token;

public interface TokenDAO extends BasicCRUDDAO<Token>
{
	String DAO_ID = "TokenDAO";
	String NAME = "token";
	
	void delete(String type, String id);
	void update(Token token);
	
	Token get(String type, String id);
	List<Token> getByType(String type);
	List<Token> getOwned(String type, long entityId);
	List<Token> getExpired();
	
	public class TokenNotFoundException extends IllegalArgumentException
	{
		public TokenNotFoundException()
		{
		}

		public TokenNotFoundException(String msg)
		{
			super(msg);
		}
	}
}
