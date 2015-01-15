/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Confirmation manager
 * 
 * @author P. Piernik
 */
public interface ConfirmationManager
{
	public void sendConfirmationRequest(String state) throws EngineException;
	public ConfirmationStatus proccessConfirmation(String token) throws EngineException;
	void rewriteRequestToken(RegistrationRequestState finalReguest, String entityId)
			throws EngineException;
	
}
