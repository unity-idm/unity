/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Confirmation manager
 * 
 * @author P. Piernik
 */
public interface ConfirmationManager
{
	public void sendConfirmationRequest(String recipientAddress, String type, String state) throws EngineException;
	public ConfirmationStatus proccessConfirmation(String token) throws EngineException;
	
}
