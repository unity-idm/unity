/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset.sms;

import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetNewCredentialUI;
import io.imunity.vaadin.auth.extensions.credreset.NewCredentialConsumer;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;

/**
 * Allows to set a new mobile number
 */
class SMSResetStep3NewMobile extends CredentialResetNewCredentialUI
{
	SMSResetStep3NewMobile(CredentialResetFlowConfig credResetConfig, CredentialEditor credEditor,
	                       NewCredentialConsumer newCredentialConsumer, String credentialConfiguration,
	                       Long entityId, NotificationPresenter notificationPresenter)
	{
		super(credResetConfig, credEditor, newCredentialConsumer, credentialConfiguration, notificationPresenter,
				entityId, credResetConfig.msg.getMessage("CredentialReset.updateMobile"));
	}
}