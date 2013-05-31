/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;

/**
 * Allows for resolving an identity into entity and also retrieving the entity's credential
 * @author K. Benedyczak
 */
public interface IdentityResolver
{
	/**
	 * Resolves an identity for performing authentication. It is guaranteed that the returned 
	 * entity has not the disabled authentication state.
	 * @param identity raw identity value
	 * @param identityTypes what are the types of the identity, in the preference order
	 * @param credentialName what credential should be provided in the returned object
	 * @return the entity info with the credential value
	 * @throws IllegalIdentityValueException if the given identity is not present in the db
	 * @throws IllegalGroupValueException 
	 * @throws IllegalTypeException 
	 */
	public EntityWithCredential resolveIdentity(String identity, String[] identityTypes, String credentialName)
		throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException;
}
