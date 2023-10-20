/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE8.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE8.SessionReinitializer;

/**
 * Process remotely obtained authentication data
 * ({@link RedirectedAuthnState}), to obtain the final decision. All external
 * authentications should be finished using this processor: both redirected and
 * local.
 */
public interface RemoteAuthnResponseProcessorEE8
{
	PostAuthenticationStepDecision processResponse(RedirectedAuthnState authnContext,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			SessionReinitializer sessionReinitializer);

	AuthenticationResult executeVerificator(Supplier<AuthenticationResult> verificator,
			AuthenticationTriggeringContext triggeringContext);

}