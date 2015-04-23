/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Provides access to authenticators. Low level, rarely useful. Needed e.g. by sandbox UI.
 * @author K. Benedyczak
 */
public interface AuthenticatorsManagement
{
	/**
	 * Resolves binding specific authenticator authN implementations for a given 
	 * list of {@link AuthenticationOptionDescription}. 
	 * @param authnList
	 * @return
	 * @throws EngineException 
	 */
	List<AuthenticationOption> getAuthenticatorUIs(List<AuthenticationOptionDescription> authnList) throws EngineException;

	/**
	 * Removes all authenticators from DB
	 * @throws EngineException
	 */
	void removeAllPersistedAuthenticators() throws EngineException;
}
