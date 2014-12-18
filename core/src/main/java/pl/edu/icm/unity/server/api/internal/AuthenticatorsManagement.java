/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;

/**
 * Provides access to authenticators. Low level, rarely useful. Needed e.g. by sandbox UI.
 * @author K. Benedyczak
 */
public interface AuthenticatorsManagement
{
	/**
	 * Resolves binding specific authenticator authN implementations for a given 
	 * list of {@link AuthenticatorSet}. 
	 * @param authnList
	 * @return
	 * @throws EngineException 
	 */
	List<Map<String, BindingAuthn>> getAuthenticatorUIs(List<AuthenticatorSet> authnList) throws EngineException;
}
