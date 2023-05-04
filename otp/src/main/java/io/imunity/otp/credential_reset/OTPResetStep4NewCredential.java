/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.credential_reset;


import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetNewCredentialUI;
import io.imunity.vaadin.auth.extensions.credreset.NewCredentialConsumer;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;

class OTPResetStep4NewCredential extends CredentialResetNewCredentialUI
{
	OTPResetStep4NewCredential(CredentialResetFlowConfig credResetConfig, CredentialEditor credEditor,
	                           NewCredentialConsumer newCredentialConsumer, String credentialConfiguration,
	                           Long entityId, NotificationPresenter notificationPresenter)
	{
		super(credResetConfig, credEditor, newCredentialConsumer, credentialConfiguration, notificationPresenter, entityId,
				credResetConfig.msg.getMessage("OTPCredentialReset.updateCredential"));
	}
}
