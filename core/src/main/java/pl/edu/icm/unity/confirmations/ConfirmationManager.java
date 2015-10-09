/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import java.util.Collection;

import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Confirmation manager
 * 
 * @author P. Piernik
 */
public interface ConfirmationManager
{
	public static final String CONFIRMATION_TOKEN_TYPE = "Confirmation";
	
	/**
	 * Send confirmation request to the user with confirmation state. 
	 * Confirmations configuration appropriate for the confirmation is used to establish
	 * message template id and notification channel id. The template is filled by manager with confirmation
	 * link and the whole message is sent via configured notification channel.  
	 * 
	 * @param state 
	 * @throws EngineException
	 */
	void sendConfirmationRequest(BaseConfirmationState state) throws EngineException;
	
	/**
	 * Process confirmation based on token. 
	 * @param tokenValue 
	 * @return Confirmation status which contains user message key and args
	 * @throws EngineException
	 */
	ConfirmationStatus processConfirmation(String tokenValue) throws EngineException;

	/**
	 * Sends confirmation messages for the values of an attribute which are verifiable, 
	 * only for unconfirmed attributes for which a confirmation was not yet sent. 
	 * In case of error only log entry is printed, no exception is thrown.
	 * @param entity
	 * @param attribute
	 * @param force if true then request is sent even if one was already sent previously
	 */
	<T> void sendVerificationQuiet(EntityParam entity, Attribute<T> attribute, boolean force);

	/**
	 * Sends confirmation messages for the identity if it requires so. Only for unconfirmed identities.
	 * In case of error only log entry is printed, no exception is thrown.
	 * @param entity
	 * @param identity
	 * @param force if true then request is sent even if one was already sent previously
	 */
	void sendVerificationQuiet(EntityParam entity, Identity identity, boolean force);

	/**
	 * see {@link #sendVerificationQuiet(EntityParam, Identity)}, the only difference is that this
	 * method throws exception.
	 * @param entity
	 * @param identity
	 * @param force if true then request is sent even if one was already sent previously
	 * @throws EngineException
	 */
	void sendVerification(EntityParam entity, Identity identity, boolean force) 
			throws EngineException;
	
	/**
	 * Sends confirmation messages for the values which requires so. Only for unconfirmed attributes.
	 * @param entity
	 * @param attribute
	 * @param force if true then request is sent even if one was already sent previously
	 */
	void sendVerificationsQuiet(EntityParam entity, Collection<? extends Attribute<?>> attributes, boolean force);

}
