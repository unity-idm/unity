/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.credreset.password;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.webui.authn.credreset.CodeVerificationCredentialResetDialog;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable;
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
public class EmailCodePasswordCredentialReset4Dialog extends CodeVerificationCredentialResetDialog
{

	public EmailCodePasswordCredentialReset4Dialog(UnityMessageSource msg, CredentialReset backend,
			CredentialEditor credEditor, String username)
	{
		super(msg, backend, credEditor, username,  3,
				new PasswordCredentialResetSettings(
						JsonUtil.parse(backend.getSettings())).getEmailSecurityCodeMsgTemplate(),
				msg.getMessage("CredentialReset.emailCode"),
				msg.getMessage("CredentialReset.resendEmailDesc"),
				msg.getMessage("CredentialReset.emailInfo"), false);

	}

	@Override
	protected void nextStep()
	{
		PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings(
				JsonUtil.parse(backend.getSettings()));
		
		if (settings.isRequireMobileConfirmation())
		{
			MobileCodePasswordCredentialReset5Dialog dialog4 = new MobileCodePasswordCredentialReset5Dialog(
					msg, backend, credEditor, username);
			dialog4.show();
		}else
		{
			// nothing more required, jump to final step 6
			CredentialResetStateVariable.inc();
			PasswordResetFinalDialog dialogFinal = new PasswordResetFinalDialog(msg,
					backend, credEditor);
			dialogFinal.show();
		}
		
	}

}
