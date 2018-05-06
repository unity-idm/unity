/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFinalDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 3rd, last step of sms credential reset pipeline. In this dialog the user must provide the new credential.
 * 
 * @author P. Piernik
 */
public class SMSCredentialResetFinalDialog extends CredentialResetFinalDialog
{

	public SMSCredentialResetFinalDialog(UnityMessageSource msg,
			CredentialReset backend, CredentialEditor credEditor)
	{
		super(msg, backend, credEditor, 2);
		
	}
}
