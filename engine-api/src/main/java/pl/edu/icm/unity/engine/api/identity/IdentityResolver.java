/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;

/**
 * Allows for resolving an identity into entity, returning also its credential.
 * <p>
 * This interface is intended for an internal use, as it performs all operations without any authorization.
 * It should be used by the authentication related components as credential validators or authenticators. 
 * 
 * @author K. Benedyczak
 */
public interface IdentityResolver
{
	/**
	 * Resolves an identity for performing authentication. It is guaranteed that the returned 
	 * entity has not the disabled authentication state.
	 * @param identity raw identity value
	 * @param identityTypes what are the types of the identity, in the preference order
	 * @param credentialName what credential should be provided in the returned object. Can be null - then no
	 * credential is set in the returned object.
	 * @return the entity info with the credential value
	 * @throws IllegalIdentityValueException if the given identity is not present in the db
	 * @throws IllegalGroupValueException 
	 * @throws IllegalTypeException 
	 * @throws EngineException 
	 */
	EntityWithCredential resolveIdentity(String identity, String[] identityTypes, String credentialName)
		throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException, EngineException;

	/**
	 * Simple version that only resolves, but doesn't establish any local credential. Useful for remote 
	 * verificators.
	 * @param identity
	 * @param identityTypes
	 * @return
	 * @throws EngineException
	 */
	long resolveIdentity(String identity, String[] identityTypes, String target, String realm) 
			throws EngineException;
	
	
	boolean isEntityEnabled(long entity);
}
