/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;
/**
 * 
 * @author P. Piernik
 *
 */
public interface ConfirmationManager
{
	public ConfirmationStatus proccessConfirmation(String token) throws EngineException;
	public void sendConfirmationRequest(String recipientAddress, String type, String state) throws EngineException;
		
	public String prepareAttributeState(String entityId, String attrType, String group) throws EngineException;
	public String prepareAttributeFromRegistrationState(String requestId, String attrType, String group)
			throws EngineException;
}
