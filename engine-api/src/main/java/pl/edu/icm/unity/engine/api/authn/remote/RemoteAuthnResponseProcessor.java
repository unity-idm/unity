/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.SessionReinitializer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;

/**
 * Process remotely obtained authentication data
 * ({@link RedirectedAuthnState}), to obtain the final decision. All external
 * authentications should be finished using this processor: both redirected and
 * local.
 */
public interface RemoteAuthnResponseProcessor
{
	PostAuthenticationStepDecision processResponse(RedirectedAuthnState authnContext,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			SessionReinitializer sessionReinitializer);

	AuthenticationResult executeVerificator(Supplier<AuthenticationResult> verificator,
			AuthenticationTriggeringContext triggeringContext);

}