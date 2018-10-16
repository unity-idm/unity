/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation;

import java.util.Collection;
import java.util.Optional;

import pl.edu.icm.unity.engine.api.confirmation.states.BaseEmailConfirmationState;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

/**
 * Confirmation manager for email attribute or identity
 * 
 * @author P. Piernik
 */
public interface EmailConfirmationManager
{
	public static final String CONFIRMATION_TOKEN_TYPE = "Confirmation";
	
	/**
	 * Send confirmation request to the user with confirmation state. 
	 * Confirmations configuration appropriate for the confirmation is used to establish
	 * message template id. The template is filled by manager with confirmation
	 * link and the whole message is sent via configured notification channel.  
	 * 
	 * @param state 
	 * @throws EngineException
	 */
	void sendConfirmationRequest(BaseEmailConfirmationState state) throws EngineException;
	
	/**
	 * Process confirmation based on token. 
	 * @param tokenValue 
	 * @return Confirmation status which contains user message key and args
	 * @throws EngineException
	 */
	WorkflowFinalizationConfiguration processConfirmation(String tokenValue) throws EngineException;

	/**
	 * Sends confirmation messages for the values of an attribute which are verifiable, 
	 * only for unconfirmed attributes for which a confirmation was not yet sent. 
	 * In case of error only log entry is printed, no exception is thrown.
	 * <p>
	 * WARNING: this method requires to set up existing transaction
	 * 
	 * @param entity
	 * @param attribute
	 * @param force if true then request is sent even if one was already sent previously
	 */
	<T> void sendVerificationQuietNoTx(EntityParam entity, Attribute attribute, boolean force);

	/**
	 * Sends confirmation messages for the values of an attribute which are verifiable, 
	 * only for unconfirmed attributes ones. 
	 * 
	 * @param entity
	 * @param attribute
	 * @throws EngineException 
	 */
	<T> void sendVerification(EntityParam entity, Attribute attribute) throws EngineException;

	/**
	 * Sends confirmation messages for the identity if it requires so. Only for unconfirmed identities.
	 * In case of error only log entry is printed, no exception is thrown.
	 * <p>
	 * WARNING: this method requires to set up existing transaction
	 * 
	 * @param entity
	 * @param identity
	 * @param force if true then request is sent even if one was already sent previously
	 */
	void sendVerificationQuietNoTx(EntityParam entity, Identity identity, boolean force);

	/**
	 * see {@link #sendVerificationQuiet(EntityParam, Identity)}, the only difference is that this
	 * method throws exception.
	 * <p>
	 * WARNING: this method requires to set up existing transaction
	 * 
	 * @param entity
	 * @param identity
	 * @param force if true then request is sent even if one was already sent previously
	 * @throws EngineException
	 */
	void sendVerificationNoTx(EntityParam entity, Identity identity, boolean force) 
			throws EngineException;

	/**
	 * see {@link #sendVerificationNoTx(EntityParam, Identity)}, the only difference is that this
	 * method starts its own transaction
	 * 
	 * @param entity
	 * @param identity
	 * @param force if true then request is sent even if one was already sent previously
	 * @throws EngineException
	 */
	void sendVerification(EntityParam entity, Identity identity) throws EngineException;

	
	/**
	 * Sends confirmation messages for the values which requires so. Only for unconfirmed attributes.
	 * <p>
	 * WARNING: this method requires to set up existing transaction
	 * 
	 * @param entity
	 * @param attribute
	 * @param force if true then request is sent even if one was already sent previously
	 */
	void sendVerificationsQuietNoTx(EntityParam entity, 
			Collection<? extends Attribute> attributes, boolean force);

	
	/**
	 * @return attribute confirmation configuration
	 */
	Optional<EmailConfirmationConfiguration> getConfirmationConfigurationForAttribute(
			String attributeName);
}
