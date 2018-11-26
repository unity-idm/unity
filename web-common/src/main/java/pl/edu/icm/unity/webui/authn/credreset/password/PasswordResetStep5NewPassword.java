/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetNewCredentialUI;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.NewCredentialConsumer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Asks for the new password
 * 
 * @author P. Piernik
 */
class PasswordResetStep5NewPassword extends CredentialResetNewCredentialUI
{
	PasswordResetStep5NewPassword(CredentialResetFlowConfig credResetConfig, CredentialEditor credEditor,
			NewCredentialConsumer newCredentialConsumer, String credentialConfiguration,
			Long entityId)
	{
		super(credResetConfig, credEditor, newCredentialConsumer, credentialConfiguration, entityId,
				credResetConfig.msg.getMessage("CredentialReset.setPassword"));
	}
}
