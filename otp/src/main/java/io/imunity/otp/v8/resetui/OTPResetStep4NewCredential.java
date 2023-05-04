/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8.resetui;

import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetNewCredentialUI;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.NewCredentialConsumer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

class OTPResetStep4NewCredential extends CredentialResetNewCredentialUI
{
	OTPResetStep4NewCredential(CredentialResetFlowConfig credResetConfig, CredentialEditor credEditor,
			NewCredentialConsumer newCredentialConsumer, String credentialConfiguration,
			Long entityId)
	{
		super(credResetConfig, credEditor, newCredentialConsumer, credentialConfiguration, entityId,
				credResetConfig.msg.getMessage("OTPCredentialReset.updateCredential"));
	}
}
