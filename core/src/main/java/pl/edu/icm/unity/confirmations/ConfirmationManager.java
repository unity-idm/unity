/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import java.util.List;

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
	 * only for unconfirmed attributes. In case of error only log entry is printed, no exception is thrown.
	 * @param entity
	 * @param attribute
	 * @param verifiableValues
	 */
	<T> void sendVerificationQuiet(EntityParam entity, Attribute<T> attribute);

	/**
	 * see {@link #sendVerificationQuiet(EntityParam, Attribute)}, the only difference is that this
	 * method throws exception.
	 * @param entity
	 * @param attribute
	 * @throws EngineException
	 */
	<T> void sendVerification(EntityParam entity, Attribute<T> attribute) throws EngineException;

	/**
	 * Sends confirmation messages for the identity if it requires so. Only for unconfirmed identities.
	 * In case of error only log entry is printed, no exception is thrown.
	 * @param entity
	 * @param identity
	 */
	void sendVerificationQuiet(EntityParam entity, Identity identity);

	/**
	 * see {@link #sendVerificationQuiet(EntityParam, Identity)}, the only difference is that this
	 * method throws exception.
	 * @param entity
	 * @param identity
	 * @throws EngineException
	 */
	void sendVerification(EntityParam entity, Identity identity) throws EngineException;
	
	/**
	 * Sends confirmation messages for the values which requires so. Only for unconfirmed attributes.
	 * @param entity
	 * @param attribute
	 * @param verifiableValues
	 */
	void sendVerificationsQuiet(EntityParam entity, List<Attribute<?>> attributes);

}
