/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import java.util.List;

import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Confirmation manager
 * 
 * @author P. Piernik
 */
public interface ConfirmationManager
{
	public static final String CONFIRMATION_TOKEN_TYPE = "Confirmation";
	
	/**
	 * Send confirmation request to the user with serialized confirmation state. 
	 * Based on state manager get confirmation configuration which contain message template id compatible 
	 * with {@link ConfirmationTemplateDef} and notification channel id. This template is fills by manager with confirmation
	 * link and whole message is send via configured notification channel.  
	 * 
	 * @param state serialized to json one of : {@link AttribiuteConfirmationState},{@link RegistrationReqAttributeState}, 
	 * {@link IdentityConfirmationState}, {@link RegistrationReqIdentityConfirmationState}  
	 * @throws EngineException
	 */
	void sendConfirmationRequest(String state) throws EngineException;
	
	/**
	 * Process confirmation based on token. 
	 * @param tokenValue 
	 * @return Confirmation status which contain user message key and args
	 * @throws EngineException
	 */
	ConfirmationStatus processConfirmation(String tokenValue) throws EngineException;

	/**
	 * Sends confirmation messages for the values which requires so. Only for unconfirmed attributes.
	 * @param entity
	 * @param attribute
	 * @param verifiableValues
	 */
	<T> void sendVerification(EntityParam entity, Attribute<T> attribute);

	/**
	 * Sends confirmation messages for the values which requires so. Only for unconfirmed attributes.
	 * @param entity
	 * @param attribute
	 * @param verifiableValues
	 */
	void sendVerifications(EntityParam entity, List<Attribute<?>> attributes);
}
