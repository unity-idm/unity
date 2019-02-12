/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;

/**
 * API for authentication flow management.
 * 
 * @author P.Piernik
 *
 */
public interface AuthenticationFlowManagement
{
	/**
	 * Add new authentication flow
	 * @param authenticatorsFlowDefinition
	 * @throws AuthorizationException
	 */
	void addAuthenticationFlow(AuthenticationFlowDefinition authenticatorsFlowDefinition) throws EngineException;
	
	/**
	 * Removes an existing authentication flow. The authentication flow must not be used by any of the endpoints,
	 * to be removed.
	 * @param toRemove authentication flow id
	 * @throws AuthorizationException
	 */
	void removeAuthenticationFlow(String toRemove) throws EngineException;
	
	/**
	 * 
	 * @return list of currently configured authentication flow
	 * @throws EngineException 
	 */
	Collection<AuthenticationFlowDefinition> getAuthenticationFlows() throws EngineException;

	/**
	 * Get authentication flow by given name
	 * @return authentication flow
	 * @throws EngineException 
	 */
	AuthenticationFlowDefinition getAuthenticationFlow(String name) throws EngineException;
	
	/**
	 * Update existing authentication flow
	 * @param authFlowdef
	 */
	void updateAuthenticationFlow(AuthenticationFlowDefinition authFlowdef) throws EngineException;
	
	/**
	 * Get user multifactor optin attribute
	 * @param entityId
	 * @return
	 * @throws EngineException
	 */
	boolean getUserMFAOptIn(long entityId) throws EngineException;
	
	/**
	 * Set user multifactor optin attribute
	 * @param entityId
	 * @param value
	 * @throws EngineException 
	 */
	void setUserMFAOptIn(long entityId, boolean value) throws EngineException;
	
	
	
}
