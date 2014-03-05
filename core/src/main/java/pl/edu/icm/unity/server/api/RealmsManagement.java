/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Authentication realm is a group of endpoints which share the same authentication context:
 * in the first place login session. Also other artifacts can have realm scope, for instance
 * the transient identities.
 *   
 * @author K. Benedyczak
 */
public interface RealmsManagement
{
	void addRealm(AuthenticationRealm realm) throws EngineException;
	
	AuthenticationRealm getRealm(String name) throws WrongArgumentException, EngineException;
	
	Collection<AuthenticationRealm> getRealms() throws EngineException;
	
	AuthenticationRealm getDefaultRealm();
	
	void updateRealm(AuthenticationRealm realm) throws EngineException;
	
	void removeRealm(String name) throws EngineException;
}
