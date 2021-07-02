/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.remote;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;

@Component
class RemoteAuthnResponseProcessor
{
	private final InteractiveAuthenticationProcessor authnProcessor;
	
	RemoteAuthnResponseProcessor(InteractiveAuthenticationProcessor authnProcessor)
	{
		this.authnProcessor = authnProcessor;
	}

	PostAuthenticationStepDecision processResponse(RemoteAuthnState authnContext,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	{
		AuthenticationResult authnResult = authnContext.processAnswer();
		FactorOrder factor = authnContext.getAuthenticationStepContext().factor; 
		PostAuthenticationStepDecision postFirstFactorDecision =
				factor == FactorOrder.FIRST ? 
						authnProcessor.processFirstFactorResult(
								authnResult, 
								authnContext.getAuthenticationStepContext(), 
								authnContext.getInitialLoginMachine(), 
								authnContext.isRememberMeEnabled(),
								httpRequest, 
								httpResponse) :
						authnProcessor.processSecondFactorResult(
								authnContext.getFirstFactorAuthnState(),
								authnResult, 
								authnContext.getAuthenticationStepContext(), 
								authnContext.getInitialLoginMachine(), 
								authnContext.isRememberMeEnabled(),
								httpRequest, 
								httpResponse);
		return postFirstFactorDecision;
	}
}
