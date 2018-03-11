/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.credreset;

import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 4th step of credential reset pipeline. In this dialog the user must provide the reset code which was
 * sent via e-mail. 
 * <p>
 * This dialog checks at startup if the username exists, channel exists and the username has address - 
 * if not then error is shown and the pipline is closed. If everything is correct the code is sent. 
 * 
 * @author P.Piernik
 *
 */
public class EmailCodeCredentialReset4Dialog extends DynamicCredentialResetDialog
{

	public EmailCodeCredentialReset4Dialog(UnityMessageSource msg, CredentialReset backend,
			CredentialEditor credEditor, String username)
	{
		super(msg, backend, credEditor, username,  3,
				backend.getSettings().getEmailSecurityCodeMsgTemplate(),
				msg.getMessage("CredentialReset.emailCode"),
				msg.getMessage("CredentialReset.resendEmailDesc"), false);

	}

	@Override
	protected void nextStep()
	{
		if (backend.getSettings().isRequireMobileConfirmation())
		{
			MobileCodeCredentialReset5Dialog dialog4 = new MobileCodeCredentialReset5Dialog(
					msg, backend, credEditor, username);
			dialog4.show();
		}else
		{
			// nothing more required, jump to final step 6
			CredentialResetStateVariable.inc();
			CredentialResetFinalDialog dialogFinal = new CredentialResetFinalDialog(msg,
					backend, credEditor);
			dialogFinal.show();
		}
		
	}

}
