/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation;

import java.util.Optional;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;

/**
 * Confirmation manager for mobile number
 * 
 * @author P. Piernik
 */
public interface MobileNumberConfirmationManager
{

	/**
	 * Send confirmation request to the user.Message template id is taken
	 * from given mobile number confirmation configuration. The template is
	 * filled by manager with confirmation code and the whole message is
	 * sent via configured notification channel. Also relevant
	 * confirmationInfo is update.
	 * 
	 * @param configEntry
	 * @param mobileToConfirm
	 * @param relatedConfirmationInfo
	 * @return sms code
	 * @throws EngineException
	 */

	SMSCode sendConfirmationRequest(MobileNumberConfirmationConfiguration configEntry,
			String mobileToConfirm, ConfirmationInfo relatedConfirmationInfo)
			throws EngineException;

	/**
	 * @return attribute confirmation configuration
	 */
	Optional<MobileNumberConfirmationConfiguration> getConfirmationConfigurationForAttribute(
			String attributeName);

}
