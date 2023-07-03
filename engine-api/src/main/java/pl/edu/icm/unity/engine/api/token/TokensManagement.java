/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.token;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;

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
 * <p>
 * As this is a low level interface it allows for external transaction steering. All methods 
 * will join an existing transaction if it is present.
 * @author K. Benedyczak
 */
public interface TokensManagement
{
	/**
	 * Adds a new token 
	 * @param type type or category of the token
	 * @param created creation timestamp. It is guaranteed to be stored with second precision, though implementation
	 * might also store the full time.
	 * @param expires precision as in created case.
	 */
	void addToken(String type, String value, EntityParam owner, byte[] contents, Date created, Date expires) 
			throws IllegalIdentityValueException, IllegalTypeException;

	/**
	 * Adds a new token without owner
	 * @param type type or category of the token
	 * @param created creation timestamp. It is guaranteed to be stored with second precision, though implementation
	 * might also store the full time.
	 * @param expires precision as in created case.
	 */
	void addToken(String type, String value, byte[] contents, Date created, Date expires) 
			throws IllegalTypeException;
	
	/**
	 * Removes the token
	 */
	void removeToken(String type, String value);
	
	/**
	 * Update the token. Only contents and expiration time can be updated - the type and value are used to look up 
	 * a token for update.
	 * @param expires if null -> leave unchanged
	 * @param contents if null -> leave unchanged
	 */
	void updateToken(String type, String value, Date expires, byte[] contents);
	
	/**
	 * Returns a specified token 
	 */
	Token getTokenById(String type, String value);
	
	/**
	 * Returns all tokens of the entity
	 */
	List<Token> getOwnedTokens(String type, EntityParam entity) 
			throws IllegalIdentityValueException, IllegalTypeException;

	/**
	 * @return all tokens of a given type
	 */
	List<Token> getAllTokens(String type); 

	/**
	 * @return all tokens 
	 */
	List<Token> getAllTokens(); 
	
	/**
	 * Adds a new listenr which is notified about expired tokens of a specified type 
	 */
	void addTokenExpirationListener(TokenExpirationListener listener, String type);
	
	/**
	 * Receives notifications about expired tokens.
	 */
	public interface TokenExpirationListener
	{
		/**
		 * Invoked just before removing the token from the database, it is guaranteed to be expired.
		 * Important: the method should return quickly, and must not manipulate DB.
		 */
		void tokenExpired(Token token);
	}
}
