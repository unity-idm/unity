/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8.resetui;

import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetCodeVerificationUI;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeConsumer;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeSender;

/**
 * On this UI the user must provide the reset code which was sent via e-mail. 
 */
class OTPResetStep3EmailCode extends CredentialResetCodeVerificationUI
{
	OTPResetStep3EmailCode(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback, 
			CodeSender codeSender)
	{
		super(credResetConfig, proceedCallback, codeSender,  
				credResetConfig.msg.getMessage("CredentialReset.emailInfo"), 
				credResetConfig.msg.getMessage("CredentialReset.emailCode"), 
				credResetConfig.msg.getMessage("CredentialReset.resendEmailDesc"));
	}
} 
