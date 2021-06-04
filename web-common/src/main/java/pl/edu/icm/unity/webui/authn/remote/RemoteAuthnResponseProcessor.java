/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.remote;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;

@Component
class RemoteAuthnResponseProcessor
{
	AuthenticationResult processResponse(RemoteAuthnState authnContext)
	{
		AuthenticationResult authnResult = authnContext.processAnswer();
		return authnResult;
		
		//TODO KB
		
	}
}
