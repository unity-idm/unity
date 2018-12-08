/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;

/**
 * Provides access to authenticators. Low level, rarely useful. Needed e.g. by
 * sandbox UI.
 * 
 * @author K. Benedyczak
 */
//TODO check this API. Looks broken or at least very strange
public interface AuthenticatorSupportManagement
{
	/**
	 * Resolves binding specific authenticator authN implementations for a given
	 * list of {@link AuthenticationFlowDefinition}.
	 */
	List<AuthenticationFlow> getAuthenticatorUIs(List<AuthenticationFlowDefinition> authnFlows, String bindingId) throws EngineException;

	List<AuthenticationFlowDefinition> resolveAllRemoteAuthenticatorFlows(String bindingId) throws EngineException;
	
	List<AuthenticationFlow> resolveAndGetAuthenticationFlows(List<String> authnOptions, String bindingId);
}
