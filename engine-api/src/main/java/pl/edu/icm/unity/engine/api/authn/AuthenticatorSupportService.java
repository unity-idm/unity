/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to authenticators. Low level, rarely useful. Needed e.g. by
 * sandbox UI.
 * 
 * @author K. Benedyczak
 */
public interface AuthenticatorSupportService
{
	List<AuthenticatorInstance> getRemoteAuthenticators(String bindingId) throws EngineException;

	List<AuthenticationFlow> getRemoteAuthenticatorsAsFlows(String bindingId) throws EngineException;

	List<AuthenticationFlow> resolveAuthenticationFlows(List<String> authnOptions, String bindingId);
	
	/**
	 * All authenticators using the provided credential are refreshed. Their endpoints have the authenticators 
	 * updated too.
	 */
	void refreshAuthenticatorsOfCredential(String credential) throws EngineException;
}
