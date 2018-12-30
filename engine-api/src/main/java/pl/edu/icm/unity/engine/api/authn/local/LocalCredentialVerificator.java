/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.local;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Verificator of local credentials. Such verificators must have 
 * local credential name set. Additionally local verificators are responsible for credential
 * handling, i.e. storing the raw credential or its configuration in DB and verification of the credential state.
 * <p>
 * Those two aspects are merged into one implementation on purpose: both local credential verification and 
 * storage of credential data in database is tightly bound together. E.g. password hashed and salted in the DB 
 * must be verified using the same hashing and salting.
 * <p>
 * The information about the supported {@link CredentialType} is created automatically from the name and description
 * of this object implementation.
 * 
 * @author K. Benedyczak
 */
public interface LocalCredentialVerificator extends CredentialVerificator
{
	/**
	 * @return the name of the credential definition associated with this verificator
	 */
	String getCredentialName();
	
	/**
	 * Sets credential definition name for this verificator. This is only required 
	 * to perform resolving of the client's identity, to get a proper credential. It is irrelevant 
	 * for credential's storage.
	 * 
	 * @param credential
	 */
	void setCredentialName(String credential);
	
	/**
	 * Prepares the credential for DB insertion. The credential value can be arbitrary, typically in JSON.
	 * Output also. For instance the input can be a password, output a hashed and salted version
	 * 
	 * @param rawCredential the new credential value
	 * @param currentCredential the existing credential, encoded in the database specific way. May be empty or 
	 * null, when there is no existing credential recorded in DB.
	 * @param verifyNew we can set new credential without its verification
	 * @return string which will be persisted in the database and will be used for verification
	 * @throws IllegalCredentialException if the new credential is not valid
	 * @throws InternalException 
	 */
	String prepareCredential(String rawCredential,  
			String currentCredential, boolean verifyNew) 
			throws IllegalCredentialException, InternalException;

	/**
	 * @param currentCredential current credential as recorded in database
	 * @return the current state of the credential, wrt the configuration of the verificator
	 * @throws InternalException 
	 */
	CredentialPublicInformation checkCredentialState(String currentCredential) throws InternalException;

	/**
	 * Returns optionally changed argument credential, which can be transformed after the change of
	 * configuration. It can be assumed that argument credential was created with some old configuration
	 * and that the current object is configured with the new one. 
	 */
	Optional<String> updateCredentialAfterConfigurationChange(String currentCredential);
	
	/**
	 * @return If the instances can be put into the {@link LocalCredentialState#outdated} state.
	 */
	boolean isSupportingInvalidation();
	
	/**
	 * This method is called only for credentials supporting invalidation.
	 * @param currentCredential the current credential value as stored in DB.
	 * @return the invalidated credential value, to be stored in database.
	 */
	String invalidate(String currentCredential);
	
	
	/**
	 * Check if credential is set 
	 * @param entity
	 * @param credentialId
	 * @return
	 * @throws EngineException 
	 */
	boolean isCredentialSet(EntityParam entity) throws EngineException;
}
