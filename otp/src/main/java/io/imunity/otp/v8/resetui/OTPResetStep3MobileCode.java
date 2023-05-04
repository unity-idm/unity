/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.v8.resetui;

import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetCodeVerificationUI;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeConsumer;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeSender;

class OTPResetStep3MobileCode extends CredentialResetCodeVerificationUI
{

	OTPResetStep3MobileCode(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback, 
			CodeSender codeSender)
	{
		super(credResetConfig, proceedCallback, codeSender, 
				credResetConfig.msg.getMessage("CredentialReset.mobileInfo"), 
				credResetConfig.msg.getMessage("CredentialReset.mobileCode"), 
				credResetConfig.msg.getMessage("CredentialReset.resendMobileDesc"));
	}
}
