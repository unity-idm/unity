/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetCodeVerificationUI;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeConsumer;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeSender;

/**
 * On this UI the user must provide the reset code which was sent via e-mail. 
 * 
 * @author P.Piernik
 */
class SMSResetStep2EmailCode extends CredentialResetCodeVerificationUI
{
	SMSResetStep2EmailCode(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback, 
			CodeSender codeSender)
	{
		super(credResetConfig, proceedCallback, codeSender,  
				credResetConfig.msg.getMessage("CredentialReset.emailInfo"), 
				credResetConfig.msg.getMessage("CredentialReset.emailCode"), 
				credResetConfig.msg.getMessage("CredentialReset.resendEmailDesc"));
	}
} 
