/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetNewCredentialUI;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.NewCredentialConsumer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Allows to set a new mobile number
 * 
 * @author P. Piernik
 */
class SMSResetStep3NewMobile extends CredentialResetNewCredentialUI
{
	SMSResetStep3NewMobile(CredentialResetFlowConfig credResetConfig, CredentialEditor credEditor,
			NewCredentialConsumer newCredentialConsumer, String credentialConfiguration,
			Long entityId)
	{
		super(credResetConfig, credEditor, newCredentialConsumer, credentialConfiguration, entityId,
				credResetConfig.msg.getMessage("CredentialReset.updateMobile"));
	}
}
