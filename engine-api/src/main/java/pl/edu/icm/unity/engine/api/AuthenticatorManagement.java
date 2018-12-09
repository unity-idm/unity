/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;

/**
 * API for authenticators management.
 * 
 * @author K. Benedyczak
 */
public interface AuthenticatorManagement
{
	/**
	 * @param bindingId if not null allows for filtering the returned list by supported binding
	 * @return list of currently configured authenticators
	 * @throws EngineException
	 */
	Collection<AuthenticatorInfo> getAuthenticators(String bindingId) throws EngineException;
	
	/**
	 * Creates a new authenticator
	 * @param typeId authenticator type id
	 * @param config configuration of authenticator as string. Should be given only for remote
	 * verificators. Otherwise should be null and the credentialId must be set.
	 * @param credentialId name of the local credential, in case when the verificator used
	 * is validating local credentials. Otherwise ignored, can be null.
	 * @return the created authenticator
	 */
	AuthenticatorInfo createAuthenticator(String id, String typeId, String config,
			String credentialId) throws EngineException;
	
	/**
	 * Updates a configuration of an existing authenticator instance
	 * @param id
	 * @param retrievalConfig configuration of retrieval as string
	 * @param localCredential name of local credential in case of local authenticator
	 * @throws EngineException
	 */
	void updateAuthenticator(String id, String verificatorConfig, String localCredential) throws EngineException;
	
	/**
	 * Removes an existing authenticator. The authenticator must not be used by any of the endpoints,
	 * to be removed.
	 * @param id
	 * @throws EngineException
	 */
	void removeAuthenticator(String id) throws EngineException;
}
