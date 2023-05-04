/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.credential_reset;

import io.imunity.vaadin.auth.extensions.credreset.CodeConsumer;
import io.imunity.vaadin.auth.extensions.credreset.CodeSender;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetCodeVerificationUI;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.elements.NotificationPresenter;

class OTPResetStep3MobileCode extends CredentialResetCodeVerificationUI
{

	OTPResetStep3MobileCode(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback,
	                        CodeSender codeSender, NotificationPresenter notificationPresenter)
	{
		super(credResetConfig, proceedCallback, codeSender, 
				credResetConfig.msg.getMessage("CredentialReset.mobileInfo"), 
				credResetConfig.msg.getMessage("CredentialReset.mobileCode"), 
				credResetConfig.msg.getMessage("CredentialReset.resendMobileDesc"),
				notificationPresenter);
	}
}
