/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;

import java.util.List;

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
	 */
	EntityWithCredential resolveIdentity(String identity, String[] identityTypes, String credentialName)
		throws EngineException;

	/**
	 * Provides information about entity including its credential 
	 */
	EntityWithCredential resolveEntity(long entityId, String credentialName)
			throws EngineException;
	
	/**
	 * Provides information about subject including its credential 
	 */
	EntityWithCredential resolveSubject(AuthenticationSubject subject, String[] identityTypes, String credentialName)
			throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException, EngineException;

	/**
	 * Provides information about subject including its credential 
	 */
	Identity resolveSubject(AuthenticationSubject subject, String identityType)
			throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException, EngineException;
	
	
	/**
	 * Simple version that only resolves, but doesn't establish any local credential. Useful for remote 
	 * verificators.
	 */
	long resolveIdentity(String identity, String[] identityTypes, String target, String realm) 
			throws EngineException;
	
	
	boolean isEntityEnabled(long entity);
	
	String getDisplayedUserName(EntityParam entity) throws EngineException;

	/**
	 * Resolves {@link EntityParam} to list of all Identities, if missing throws exception
	 * @param entity Describes search criteria
	 * @return List of {@link Identity}
	 * @throws IllegalIdentityValueException for missing entity
	 */
	List<Identity> getIdentitiesForEntity(EntityParam entity) throws IllegalIdentityValueException;

	/**
	 * Insert identity in DB.
	 *
	 * @param toAdd Describes {@link Identity} details
	 * @param entity {@link EntityParam} describing search criteria
	 * @return Created {@link Identity}
	 * @throws IllegalIdentityValueException In case of basic rules check failure.
	 */
	Identity insertIdentity(IdentityParam toAdd, EntityParam entity) throws IllegalIdentityValueException;
}
