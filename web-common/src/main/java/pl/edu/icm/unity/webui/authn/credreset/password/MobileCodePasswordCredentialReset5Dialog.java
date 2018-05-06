/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.credreset.password;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.webui.authn.credreset.CodeVerificationCredentialResetDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 5th step of credential reset pipeline. In this dialog the user must provide the reset code which was
 * sent via sms. 
 * <p>
 * This dialog checks at startup if the username exists, channel exists and the username has address - 
 * if not then error is shown and the pipline is closed. If everything is correct the code is sent. 
 * 
 * @author P.Piernik
 *
 */
public class MobileCodePasswordCredentialReset5Dialog extends CodeVerificationCredentialResetDialog
{

	public MobileCodePasswordCredentialReset5Dialog(UnityMessageSource msg, CredentialReset backend,
			CredentialEditor credEditor, String username)
	{
		super(msg, backend, credEditor, username,  4,
				new PasswordCredentialResetSettings(
						JsonUtil.parse(backend.getSettings())).getMobileSecurityCodeMsgTemplate(),
				msg.getMessage("CredentialReset.mobileCode"),
				msg.getMessage("CredentialReset.resendMobileDesc"),
				msg.getMessage("CredentialReset.mobileInfo"), true);

	}

	@Override
	protected void nextStep()
	{
		PasswordResetFinalDialog dialogFinal = new PasswordResetFinalDialog(msg, backend, credEditor);
		dialogFinal.show();
		
	}

}
