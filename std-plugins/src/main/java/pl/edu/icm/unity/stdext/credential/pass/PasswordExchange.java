/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;

/**
 * Allows for exchanging a regular passphrase.
 * @author K. Benedyczak
 */
public interface PasswordExchange extends CredentialExchange
{
	public static final String ID = "password exchange";
	
	/**
	 * Verifies the user provided credentials.
	 * @param username
	 * @param password
	 * @param sandboxCallback typically null, if in sandbox mode provides callback.
	 * @return
	 */
	public AuthenticationResult checkPassword(String username, String password, 
			SandboxAuthnResultCallback sandboxCallback) throws AuthenticationException; 
	
	/**
	 * @return credential reset backend
	 */
	public CredentialReset getCredentialResetBackend();
}
