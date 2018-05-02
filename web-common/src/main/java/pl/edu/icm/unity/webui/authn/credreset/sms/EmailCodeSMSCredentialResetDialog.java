/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.webui.authn.credreset.CodeVerificationCredentialResetDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 2nd step of sms credential reset pipeline. In this dialog the user must provide the reset code which was
 * sent via e-mail. 
 * <p>
 * This dialog checks at startup if the username exists, channel exists and the username has address - 
 * if not then error is shown and the pipline is closed. If everything is correct the code is sent. 
 * 
 * @author P.Piernik
 *
 */
public class EmailCodeSMSCredentialResetDialog extends CodeVerificationCredentialResetDialog
{

	public EmailCodeSMSCredentialResetDialog(UnityMessageSource msg, CredentialReset backend,
			CredentialEditor credEditor, String username)
	{
		super(msg, backend, credEditor, username,  1,
				new SMSCredentialRecoverySettings(
						JsonUtil.parse(backend.getSettings())).getEmailSecurityCodeMsgTemplate(),
				msg.getMessage("CredentialReset.emailCode"),
				msg.getMessage("CredentialReset.resendEmailDesc"),
				msg.getMessage("CredentialReset.emailInfo"), false);

	}

	@Override
	protected void nextStep()
	{
		SMSCredentialResetFinalDialog dialogFinal = new SMSCredentialResetFinalDialog(msg,
				backend, credEditor);
		dialogFinal.show();		
	}
}
