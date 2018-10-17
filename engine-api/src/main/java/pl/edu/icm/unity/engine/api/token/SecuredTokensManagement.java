/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.token;

import java.util.List;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

/** Secured tokens API allows for manipulating generic tokens. User with "System Manager" authorization role can manage 
 * all tokens. All other users can manage only their own token
 * 
 * @author P.Piernik
 *
 */
public interface SecuredTokensManagement
{
		
	/**
	 * User with "System Manager" authorization role can get all tokens. All other users can get only own tokens
	 * @param type. If null all types will be return
	 * @return all tokens of a given type
	 * @throws EngineException
	 */
	List<Token> getAllTokens(String type) throws EngineException;

	/**
	 *User with "System Manager" authorization role can get all tokens of all entities. All other users can get only own tokens.
	 * @param type. If null all types will be return
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	List<Token> getOwnedTokens(String type, EntityParam entity)
			throws EngineException;
	
	/**
	 * Returns all tokens of the logged entity
	 * 
	 * @param type. If null all types will be return
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	List<Token> getOwnedTokens(String type) throws EngineException;

	/**
	 * User with "System Manager" authorization role can remove all tokens. All other users can remove only own tokens
	 * 
	 * @param type
	 * @param value
	 * @throws AuthorizationException
	 */
	void removeToken(String type, String value) throws EngineException;
	
	
}
