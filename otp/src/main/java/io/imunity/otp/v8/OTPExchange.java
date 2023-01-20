/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;

public interface OTPExchange extends CredentialExchange
{
	public static final String ID = "otp-exchange";
	
	public AuthenticationResult verifyCode(String code, AuthenticationSubject subject);
	
	public OTPCredentialReset getCredentialResetBackend();

	public int getCodeLength();
}
