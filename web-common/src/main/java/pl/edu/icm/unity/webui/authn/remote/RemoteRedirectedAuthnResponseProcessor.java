/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.remote;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.utils.LogRecorder;

@Component
class RemoteRedirectedAuthnResponseProcessor
{
	private final InteractiveAuthenticationProcessor authnProcessor;
	
	RemoteRedirectedAuthnResponseProcessor(InteractiveAuthenticationProcessor authnProcessor)
	{
		this.authnProcessor = authnProcessor;
	}

	PostAuthenticationStepDecision processResponse(RemoteAuthnState authnContext,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	{
		AuthenticationResult authnResult = executeVerificator(authnContext, httpRequest.getSession().getId());
		if (authnContext.getAuthenticationTriggeringContext().isRegistrationTriggered())
		{
			return authnProcessor.processRemoteRegistrationResult(authnResult, 
					authnContext.getAuthenticationStepContext(), 
					authnContext.getInitialLoginMachine(), 
					httpRequest);
		} else if (authnContext.getAuthenticationTriggeringContext().isSandboxTriggered())
		{
			return processSandboxAuthenticationResult(authnContext, httpRequest, authnResult);
		} else
		{
			return processRegularAuthenticationResult(authnContext, httpRequest, httpResponse, authnResult);
		}
	}

	private AuthenticationResult executeVerificator(RemoteAuthnState authnContext, String sessionId)
	{
		boolean sandboxMode = authnContext.getAuthenticationTriggeringContext().isSandboxTriggered();
		return sandboxMode ? executeVerificatorInSandboxMode(authnContext, sessionId) : authnContext.processAnswer(); 
	}

	private AuthenticationResult executeVerificatorInSandboxMode(RemoteAuthnState authnContext, String sessionId)
	{
		LogRecorder logRecorder = new LogRecorder(Log.REMOTE_AUTHENTICATION_RELATED_FACILITIES);
		logRecorder.startLogRecording();
		AuthenticationResult authnResult = authnContext.processAnswer();
		logRecorder.stopLogRecording();
		RemoteAuthenticationResult remoteAuthenticationResult = authnResult.asRemote();
		SandboxAuthnContext sandboxAuthnInfo = remoteAuthenticationResult.getStatus() == Status.deny ?
				RemoteSandboxAuthnContext.failedAuthn(
						remoteAuthenticationResult.getErrorResult().cause,
						logRecorder.getCapturedLogs().toString(),
						remoteAuthenticationResult.getRemotelyAuthenticatedPrincipal().getAuthnInput()) 
				: RemoteSandboxAuthnContext.succeededAuthn(
						remoteAuthenticationResult.getRemotelyAuthenticatedPrincipal(), 
						logRecorder.getCapturedLogs().toString());
		authnContext.getAuthenticationTriggeringContext().sandboxRouter.firePartialEvent(
				new SandboxAuthnEvent(sandboxAuthnInfo, sessionId));
		return authnResult;
	}
	
	private PostAuthenticationStepDecision processRegularAuthenticationResult(RemoteAuthnState authnContext, 
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, AuthenticationResult authnResult)
	{
		FactorOrder factor = authnContext.getAuthenticationStepContext().factor; 
		return factor == FactorOrder.FIRST ? 
					authnProcessor.processFirstFactorResult(
							authnResult, 
							authnContext.getAuthenticationStepContext(), 
							authnContext.getInitialLoginMachine(), 
							authnContext.getAuthenticationTriggeringContext().rememberMeSet,
							httpRequest, 
							httpResponse) :
					authnProcessor.processSecondFactorResult(
							authnContext.getAuthenticationTriggeringContext().firstFactorAuthnState,
							authnResult, 
							authnContext.getAuthenticationStepContext(), 
							authnContext.getInitialLoginMachine(), 
							authnContext.getAuthenticationTriggeringContext().rememberMeSet,
							httpRequest, 
							httpResponse);
	}
	
	private PostAuthenticationStepDecision processSandboxAuthenticationResult(RemoteAuthnState authnContext, 
			HttpServletRequest httpRequest,
			AuthenticationResult authnResult)
	{
		FactorOrder factor = authnContext.getAuthenticationStepContext().factor; 
		return factor == FactorOrder.FIRST ? 
					authnProcessor.processFirstFactorSandboxAuthnResult(
							authnResult, 
							authnContext.getAuthenticationStepContext(), 
							authnContext.getInitialLoginMachine(), 
							httpRequest, 
							authnContext.getAuthenticationTriggeringContext().sandboxRouter) :
					authnProcessor.processSecondFactorSandboxAuthnResult(
							authnContext.getAuthenticationTriggeringContext().firstFactorAuthnState,
							authnResult, 
							authnContext.getAuthenticationStepContext(), 
							authnContext.getInitialLoginMachine(), 
							httpRequest, 
							authnContext.getAuthenticationTriggeringContext().sandboxRouter);
	}

}
