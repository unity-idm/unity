/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;

/**
 * Allows for exchanging a sms code. Standard flow is simple - first we send
 * code to the user and next we verify that the code typed by user is the same as
 * sent code. Sms limit can be checked and when is exceeded then send code with force option can
 * be used. 
 * 
 * @author P.Piernik
 *
 */
public interface SMSExchange extends CredentialExchange
{
	public static final String ID = "sms exchange";
	
	/**
	 * Send code to the user
	 * @param force forcing auth sms limit omitting
	 * @param sandboxCallback typically null, if in sandbox mode provides callback.
	 */
	SMSCode sendCode(AuthenticationSubject username, boolean force) throws EngineException;
	
	/**
	 * Verifies the user provided sms code.
	 */
	AuthenticationResult verifyCode(SMSCode sentCode, String codeFromUser,
			AuthenticationSubject subject);
	
	/**
	 * Check if sms authn sending limit is exceeded
	 */
	boolean isAuthSMSLimitExceeded(AuthenticationSubject username);	
	
	/**
	 * @return credential reset backend
	 */
	SMSCredentialResetImpl getSMSCredentialResetBackend();

	
}
