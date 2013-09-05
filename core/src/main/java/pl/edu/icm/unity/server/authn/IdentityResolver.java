/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
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
	 * @param credentialName what credential should be provided in the returned object
	 * @return the entity info with the credential value
	 * @throws IllegalIdentityValueException if the given identity is not present in the db
	 * @throws IllegalGroupValueException 
	 * @throws IllegalTypeException 
	 */
	public EntityWithCredential resolveIdentity(String identity, String[] identityTypes, String credentialName)
		throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException;
	
	/**
	 * Updates the credential in DB. This feature is required to perform a sort of callback: credentials
	 * may need to update themselves in DB, e.g. to invalidate them in case when it is detected during login 
	 * that the current password is not valid anymore.
	 * 
	 * @param entityId
	 * @param credentialName
	 * @param value
	 * @throws EngineException
	 */
	public void updateCredential(long entityId, String credentialName, String value) 
			throws IllegalAttributeValueException, IllegalTypeException, 
			IllegalAttributeTypeException, IllegalGroupValueException; 
}
