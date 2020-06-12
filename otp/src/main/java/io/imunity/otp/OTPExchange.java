/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;

interface OTPExchange extends CredentialExchange
{
	static final String ID = "otp-exchange";
	
	AuthenticationResult verifyCode(String codeFromUser, String username, SandboxAuthnResultCallback sandboxCallback);
	
	OTPCredentialReset getCredentialResetBackend();

	int getCodeLength();
}
