/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import io.imunity.otp.credential_reset.OTPCredentialReset;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;

public interface OTPExchange extends CredentialExchange
{
	String ID = "otp-exchange";
	
	AuthenticationResult verifyCode(String code, AuthenticationSubject subject);
	
	OTPCredentialReset getCredentialResetBackend();

	int getCodeLength();
}
