/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.DescribedObject;
import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Handler for locally stored credentials. Offers methods to get description of the supported credential type
 * and can be used to prepare a raw credential for DB insertion and checking it for correctness. 
 * It might be configured, e.g. to set credential restrictions (as minimal password length in case of 
 * password credential) via the standard {@link JsonSerializable} interface.
 * @author K. Benedyczak
 */
public interface LocalCredentialHandler extends JsonSerializable, DescribedObject
{
	/**
	 * @return credential type supported by this verificator
	 */
	public CredentialType getCredentialType(); 

	/**
	 * Prepares the credential for DB insertion. The credential value can be arbitrary, typically in JSON.
	 * Output also. For instance the input can be a password, output a hashed and salted version
	 * 
	 * @param rawCredential the new credential value
	 * @param currentCredential the existing credential, encoded in the database specific way
	 * @return string which will be persisted in the database and will be used for verification
	 * @throws IllegalCredentialException if the new credential is not valid
	 */
	public String prepareCredential(String rawCredential, String currentCredential) throws IllegalCredentialException;
	
	/**
	 * @param currentCredential current credential as recorded in database
	 * @return the current state of the credential, wrt the configuration of the verificator
	 */
	public LocalCredentialState checkCredentialState(String currentCredential);
}
