/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Tokens API allows for manipulating generic tokens. Token is a piece of information which is:
 * <ul>
 *  <li> bound to an entity
 *  <li> has unique identifier
 *  <li> has type (e.g. OAuth access token)
 *  <li> can have expiration time, when it is removed
 *  <li> can contain additional, type specific data
 * </ul>
 * Tokens are used by both engine and endpoint extension modules.
 *  
 * @author K. Benedyczak
 */
public interface TokensManagement
{
	/**
	 * Adds a new token
	 * @param token
	 * @throws WrongArgumentException
	 * @throws IllegalTypeException 
	 * @throws IllegalIdentityValueException 
	 */
	void addToken(String type, String value, EntityParam owner, byte[] contents, Date expires) 
			throws WrongArgumentException, IllegalIdentityValueException, IllegalTypeException;
	
	/**
	 * Removes the token
	 * @param type
	 * @param value
	 * @throws WrongArgumentException
	 */
	void removeToken(String type, String value) throws WrongArgumentException;
	
	/**
	 * Update the token. Only contents can be updated - the type and value are used to look up 
	 * a token for update.
	 * @param type
	 * @param value
	 * @param contents
	 * @throws WrongArgumentException
	 */
	void updateToken(String type, String value, byte[] contents) throws WrongArgumentException;
	
	/**
	 * Returns a specified token 
	 * @param type
	 * @param value
	 * @return
	 * @throws WrongArgumentException 
	 */
	Token getTokenById(String type, String value) throws WrongArgumentException;
	
	/**
	 * Returns all tokens of the entity
	 * @param type
	 * @param entity
	 * @return
	 * @throws IllegalTypeException 
	 * @throws IllegalIdentityValueException 
	 */
	List<Token> getOwnedTokens(String type, EntityParam entity) 
			throws IllegalIdentityValueException, IllegalTypeException;
	
	/**
	 * Adds a new listenr which is notified about expired tokens of a specified type 
	 * @param listener
	 */
	void addTokenExpirationListener(TokenExpirationListener listener, String type);
	
	/**
	 * Receives notifications about expired tokens.
	 * 
	 * @author K. Benedyczak
	 */
	public interface TokenExpirationListener
	{
		/**
		 * Invoked just before removing the token from the database, it is guaranteed to be expired.
		 * Important: the method should return quickly, and must not manipulate DB.
		 * @param token
		 */
		void tokenExpired(Token token);
	}
}
