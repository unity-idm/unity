/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;

/**
 * Interface for validation of an access token obtained by a credential retrieval.
 * @author K. Benedyczak
 */
public interface AccessTokenExchange extends CredentialExchange
{
	public static final String ID = "access token exchange";
	
	public AuthenticationResult checkToken(BearerAccessToken token, SandboxAuthnResultCallback sandboxCallback) 
			throws EngineException;
}
