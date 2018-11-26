/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.credreset.password;

import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetCodeVerificationUI;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeConsumer;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeSender;

/**
 * On this UI user must provide the reset code which was sent via sms. 
 * 
 * @author P.Piernik
 */
class PasswordResetStep4MobileCode extends CredentialResetCodeVerificationUI
{

	PasswordResetStep4MobileCode(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback, 
			CodeSender codeSender)
	{
		super(credResetConfig, proceedCallback, codeSender, 
				credResetConfig.msg.getMessage("CredentialReset.mobileInfo"), 
				credResetConfig.msg.getMessage("CredentialReset.mobileCode"), 
				credResetConfig.msg.getMessage("CredentialReset.resendMobileDesc"));
	}
}
