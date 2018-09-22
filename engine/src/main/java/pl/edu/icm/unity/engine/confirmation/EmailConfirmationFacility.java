/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation;

import pl.edu.icm.unity.engine.api.confirmation.states.BaseEmailConfirmationState;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.DescribedObject;

/**
 * Implementations are providing confirmation functionality.
 *
 * @author P. Piernik
 *
 */
public interface EmailConfirmationFacility<T extends BaseEmailConfirmationState> extends DescribedObject
{
	/**
	 * Try to confirm verifiable element based on state. 
	 * @param state
	 * @return
	 * @throws EngineException
	 */
	WorkflowFinalizationConfiguration processConfirmation(String state) throws EngineException;
	
	/**
	 * Update verifiable element set as unconfirmed and increase the value of
	 * sent request. 
	 * @param state
	 * @throws EngineException
	 */
	void processAfterSendRequest(String state) throws EngineException;
	
	/**
	 * Returns true if the given candidate state looks as a duplicate: is applicable to the same object 
	 * (user or registration request) and has the same value.
	 * @param state
	 * @return
	 */
	boolean isDuplicate(T base, String candidate);
	
	/**
	 * Parses the given state string token
	 * @throws {@link IllegalArgumentException} if the token is of incompatible type 
	 */
	T parseState(String contents);
}
