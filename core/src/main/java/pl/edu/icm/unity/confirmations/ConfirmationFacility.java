/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.DescribedObject;

/**
 * Implementations are providing confirmation functionality.
 *
 * @author P. Piernik
 *
 */
public interface ConfirmationFacility extends DescribedObject
{
	/**
	 * Try to confirm verifiable element based on state. 
	 * @param state
	 * @return
	 * @throws EngineException
	 */
	public ConfirmationStatus processConfirmation(String state) throws EngineException;
	
	/**
	 * Update verifiable element set as unconfirmed and increase the value of
	 * sent request. 
	 * @param state
	 * @throws EngineException
	 */
	public void processAfterSendRequest(String state) throws EngineException;
}
