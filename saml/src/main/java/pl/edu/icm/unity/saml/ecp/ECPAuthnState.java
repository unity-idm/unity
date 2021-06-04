/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.util.function.Function;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

/**
 * State of the ECP authentication, i.e. records the info about a request provided via PAOS, 
 * to be correlated with a further response.
 * @author K. Benedyczak
 */
public class ECPAuthnState extends RemoteAuthnState
{
	private String requestId;

	public ECPAuthnState(AuthenticationOptionKey authenticatorOptionId, 
			Function<RemoteAuthnState, AuthenticationResult> responseHandler) 
	{
		super(authenticatorOptionId, responseHandler);
	}

	public void setRequestId(String requestId)
	{
		this.requestId = requestId;
	}

	public String getRequestId()
	{
		return requestId;
	}
}
