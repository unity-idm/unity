/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.token;

import java.util.List;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.EntityParam;

/** Secured tokens API allows for manipulating generic tokens. User with maintenance capability can manage 
 * all tokens. All other users can manage only their own token
 * 
 * @author P.Piernik
 *
 */
public interface SecuredTokensManagement
{
		
	/**
	 * @param type. If null all types will be return
	 * @return all tokens of a given type
	 * @throws IllegalTypeException
	 * @throws IllegalIdentityValueException
	 * @throws AuthorizationException
	 */
	List<Token> getAllTokens(String type) throws IllegalIdentityValueException,
			IllegalTypeException, AuthorizationException;

	/**
	 * Returns all tokens of the entity
	 * 
	 * @param type. If null all types will be return
	 * @param entity
	 * @return
	 * @throws IllegalTypeException
	 * @throws IllegalIdentityValueException
	 * @throws AuthorizationException
	 */
	List<Token> getOwnedTokens(String type, EntityParam entity)
			throws IllegalIdentityValueException, IllegalTypeException,
			AuthorizationException;

	/**
	 * Returns all tokens of the logged entity
	 * 
	 * @param type. If null all types will be return
	 * @param entity
	 * @return
	 * @throws IllegalTypeException
	 * @throws IllegalIdentityValueException
	 * @throws AuthorizationException
	 */
	List<Token> getOwnedTokens(String type) throws IllegalIdentityValueException,
			IllegalTypeException, AuthorizationException;

	/**
	 * Removes the token
	 * 
	 * @param type
	 * @param value
	 * @throws AuthorizationException
	 */
	void removeToken(String type, String value) throws AuthorizationException;
	
	
}
