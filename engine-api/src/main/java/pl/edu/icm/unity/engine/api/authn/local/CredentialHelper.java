/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.local;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Allows for performing various credential related operations which needs to be handled internally,
 * not via the public API.
 * <p>
 * This interface is intended for an internal use, as it performs all operations without any authorization.
 * It should be used by the authentication related components as credential validators or authenticators. 
 * 
 * @author K. Benedyczak
 */
public interface CredentialHelper
{
	/**
	 * Updates the credential in DB. This feature is required to perform a sort of callback: credentials
	 * may need to update themselves in DB, e.g. to invalidate them in case when it is detected during login 
	 * that the current password is not valid anymore.
	 * <p>
	 * IMPORTANT: the last argument must be given in a database format, i.e. must be already result of processing
	 * by an appropriate credential handler. Therefore this method is useful to be called from a 
	 * verificator itself.
	 * 
	 * @param entityId
	 * @param credentialName
	 * @param dbEncodedCredentialState
	 * @throws EngineException
	 */
	void updateCredential(long entityId, String credentialName, String dbEncodedCredentialState) 
			throws EngineException;
	
	/**
	 * Updates the credential in DB. This is the same code as 
	 * {@link IdentitiesManagement#setEntityCredential(pl.edu.icm.unity.types.basic.EntityParam, String, String)}
	 * but requires no authorization.
	 * 
	 * @param entityId
	 * @param credentialName
	 * @param value
	 * @throws EngineException
	 */
	void setCredential(long entityId, String credentialName, String value,
			LocalCredentialVerificator handler) throws EngineException; 
	
	/**
	 * Check if credential is set 
	 * @param entity
	 * @param credentialId
	 * @return
	 * @throws EngineException
	 */
	boolean isCredentialSet(EntityParam entity, String credentialId)
			throws EngineException;

	/**
	 * Get credential definitions as map
	 * @return
	 * @throws EngineException
	 */
	Map<String, CredentialDefinition> getCredentialDefinitions() throws EngineException;
}
