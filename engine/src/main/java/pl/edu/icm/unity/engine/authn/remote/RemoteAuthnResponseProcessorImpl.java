/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn.remote;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResponseProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.utils.LogRecorder;

@Component
class RemoteAuthnResponseProcessorImpl implements RemoteAuthnResponseProcessor
{
	private final InteractiveAuthenticationProcessor authnProcessor;
	
	RemoteAuthnResponseProcessorImpl(InteractiveAuthenticationProcessor authnProcessor)
	{
		this.authnProcessor = authnProcessor;
	}

	@Override
	public PostAuthenticationStepDecision processResponse(RemoteAuthnState authnContext,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	{
		AuthenticationTriggeringContext triggeringContext = authnContext.getAuthenticationTriggeringContext();
		return triggeringContext.isSandboxTriggered() ? 
			processResponseInSandboxMode(authnContext, httpRequest, triggeringContext) :
			processResponseInProductionMode(authnContext, httpRequest, httpResponse, triggeringContext);
	}

	private PostAuthenticationStepDecision processResponseInProductionMode(RemoteAuthnState authnContext, 
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, AuthenticationTriggeringContext triggeringContext)
	{
		AuthenticationResult authnResult = authnContext.processAnswer();
		if (triggeringContext.isRegistrationTriggered())
		{
			return authnProcessor.processRemoteRegistrationResult(authnResult, 
					authnContext.getAuthenticationStepContext(), 
					authnContext.getInitialLoginMachine(), 
					httpRequest);
		} else
		{
			return processRegularAuthenticationResult(authnContext, httpRequest, httpResponse, authnResult);
		}
	}

	private PostAuthenticationStepDecision processResponseInSandboxMode(RemoteAuthnState authnContext, 
			HttpServletRequest httpRequest,
			AuthenticationTriggeringContext triggeringContext)
	{
		SandboxAuthenticationResult authnResult = executeVerificatorInSandboxMode(
				authnContext::processAnswer, triggeringContext);
		return processSandboxAuthenticationResult(authnContext, httpRequest, authnResult);
	}

	@Override
	public AuthenticationResult executeVerificator(Supplier<AuthenticationResult> verificator, 
			AuthenticationTriggeringContext triggeringContext)
	{
		boolean sandboxMode = triggeringContext.isSandboxTriggered();
		return sandboxMode ? 
				executeVerificatorInSandboxMode(verificator, triggeringContext) 
				: verificator.get(); 
	}
	
	private SandboxAuthenticationResult executeVerificatorInSandboxMode(Supplier<AuthenticationResult> verificator, 
			AuthenticationTriggeringContext triggeringContext)
	{
		LogRecorder logRecorder = new LogRecorder(Log.REMOTE_AUTHENTICATION_RELATED_FACILITIES);
		logRecorder.startLogRecording();
		AuthenticationResult authnResult = verificator.get();
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
		return new SandboxAuthenticationResult(remoteAuthenticationResult, sandboxAuthnInfo);
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
			SandboxAuthenticationResult authnResult)
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
