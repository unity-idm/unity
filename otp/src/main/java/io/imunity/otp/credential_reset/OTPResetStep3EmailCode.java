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

/**
 * On this UI the user must provide the reset code which was sent via e-mail. 
 */
class OTPResetStep3EmailCode extends CredentialResetCodeVerificationUI
{
	OTPResetStep3EmailCode(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback,
	                       CodeSender codeSender, NotificationPresenter notificationPresenter)
	{
		super(credResetConfig, proceedCallback, codeSender,  
				credResetConfig.msg.getMessage("CredentialReset.emailInfo"), 
				credResetConfig.msg.getMessage("CredentialReset.emailCode"), 
				credResetConfig.msg.getMessage("CredentialReset.resendEmailDesc"),
				notificationPresenter);
	}
} 
