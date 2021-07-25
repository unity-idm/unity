/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;

/**
 * Process remotely obtained authentication data ({@link RemoteAuthnState}), to obtain the final decision.
 * All external authentications should be finished using this processor: both redirected and local.
 */
public interface RemoteAuthnResponseProcessor
{
	PostAuthenticationStepDecision processResponse(RemoteAuthnState authnContext, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse);
	
	AuthenticationResult executeVerificator(Supplier<AuthenticationResult> verificator, 
			AuthenticationTriggeringContext triggeringContext, String sessionId);

}