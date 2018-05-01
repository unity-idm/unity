/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Allows for exchanging a sms code.
 * @author P.Piernik
 *
 */
public interface SMSExchange extends CredentialExchange
{
	public static final String ID = "sms exchange";
	
	/**
	 * Send code to the user
	 * @param username
	 * @param password
	 * @param sandboxCallback typically null, if in sandbox mode provides callback.
	 * @return
	 */
	
	SMSCode sendCode(String username) throws EngineException;
	
	/**
	 * Verifies the user provided code.
	 * @param username
	 * @param password
	 * @param sandboxCallback typically null, if in sandbox mode provides callback.
	 * @return
	 */
	AuthenticationResult verifyCode(SMSCode sentCode, String codeFromUser,
			String username, SandboxAuthnResultCallback sandboxCallback);
	
	
	/**
	 * @return credential reset backend
	 */
	SMSCredentialResetImpl getSMSCredentialResetBackend();
}
