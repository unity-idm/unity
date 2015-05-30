/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;

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
			SandboxAuthnResultCallback sandboxCallback); 
	
	/**
	 * @return credential reset backend
	 */
	public CredentialReset getCredentialResetBackend();
}
