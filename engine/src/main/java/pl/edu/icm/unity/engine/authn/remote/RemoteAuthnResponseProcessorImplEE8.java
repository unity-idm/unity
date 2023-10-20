/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn.remote;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE8;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE8.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE8.SessionReinitializer;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResponseProcessorEE8;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.utils.LogRecorder;

@Component
class RemoteAuthnResponseProcessorImplEE8 implements RemoteAuthnResponseProcessorEE8
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, RemoteAuthnResponseProcessorImplEE8.class);
	private final InteractiveAuthenticationProcessorEE8 authnProcessor;
	
	RemoteAuthnResponseProcessorImplEE8(InteractiveAuthenticationProcessorEE8 authnProcessor)
	{
		this.authnProcessor = authnProcessor;
	}

	@Override
	public PostAuthenticationStepDecision processResponse(RedirectedAuthnState authnContext,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			SessionReinitializer sessionReinitializer)
	{
		AuthenticationTriggeringContext triggeringContext = authnContext.getAuthenticationTriggeringContext();
		return triggeringContext.isSandboxTriggered() ? 
			processResponseInSandboxMode(authnContext, httpRequest, triggeringContext) :
			processResponseInProductionMode(authnContext, httpRequest, httpResponse, triggeringContext, 
					sessionReinitializer);
	}

	private PostAuthenticationStepDecision processResponseInProductionMode(RedirectedAuthnState authnContext,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			AuthenticationTriggeringContext triggeringContext,
			SessionReinitializer sessionReinitializer)
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
			return processRegularAuthenticationResult(authnContext, httpRequest, httpResponse, authnResult,
					sessionReinitializer);
		}
	}

	private PostAuthenticationStepDecision processResponseInSandboxMode(RedirectedAuthnState authnContext, 
			HttpServletRequest httpRequest,
			AuthenticationTriggeringContext triggeringContext)
	{
		
		SandboxAuthenticationResult authnResult = executeVerificatorInSandboxMode(
				authnContext::processAnswer, triggeringContext);
		if (triggeringContext.isRegistrationTriggered())
		{
			return authnProcessor.processRemoteRegistrationResult(authnResult, 
					authnContext.getAuthenticationStepContext(), 
					authnContext.getInitialLoginMachine(), 
					httpRequest);
		} 
		
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
		AuthenticationResult authnResult;
		try
		{
			authnResult = verificator.get();
		} catch (Exception e)
		{
			return handleVerificatorException(logRecorder, e);
		} finally 
		{
			logRecorder.stopLogRecording();
		}
		
		if (!authnResult.isRemote())
			return handleNonRemoteSandboxResult(logRecorder, authnResult);
		
		return handleRemoteSandboxResult(logRecorder, authnResult.asRemote());
	}

	private SandboxAuthenticationResult handleRemoteSandboxResult(LogRecorder logRecorder,
			RemoteAuthenticationResult remoteAuthenticationResult)
	{
		RemotelyAuthenticatedPrincipal remotePrincipal = remoteAuthenticationResult.getRemotelyAuthenticatedPrincipal();
		SandboxAuthnContext sandboxAuthnInfo = remoteAuthenticationResult.getStatus() == Status.deny ?
				RemoteSandboxAuthnContext.failedAuthn(
						remoteAuthenticationResult.getErrorResult().cause,
						logRecorder.getCapturedLogs().toString(),
						remotePrincipal != null ? remotePrincipal.getAuthnInput() : null) 
				: RemoteSandboxAuthnContext.succeededAuthn(
						remoteAuthenticationResult.getRemotelyAuthenticatedPrincipal(), 
						logRecorder.getCapturedLogs().toString());
		return new SandboxAuthenticationResult(remoteAuthenticationResult, sandboxAuthnInfo);
	}

	private SandboxAuthenticationResult handleVerificatorException(LogRecorder logRecorder, Exception e)
	{
		log.error("Verificator has thrown an exception (sandbox execution)", e);
		return new SandboxAuthenticationResult(RemoteAuthenticationResult.failed(e), 
				RemoteSandboxAuthnContext.failedAuthn(
						e, logRecorder.getCapturedLogs().toString(), null));
	}

	private SandboxAuthenticationResult handleNonRemoteSandboxResult(LogRecorder logRecorder, AuthenticationResult authnResult)
	{
		log.error("Got non-remote authn result in sandbox mode: {}, returning failure. That's a bug.",
				authnResult);
		return new SandboxAuthenticationResult(RemoteAuthenticationResult.failed(new IllegalStateException(
				"Got non-remote authn result in sandbox mode: " + 
				authnResult + ", returning failure. That's a bug.")), 
				RemoteSandboxAuthnContext.failedAuthn(
						new IllegalStateException(
								"Got non-remote authn result in sandbox mode: " + 
								authnResult + ", returning failure. That's a bug."),
							logRecorder.getCapturedLogs().toString(),
							null));
	}
	
	private PostAuthenticationStepDecision processRegularAuthenticationResult(RedirectedAuthnState authnContext,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			AuthenticationResult authnResult,
			SessionReinitializer sessionReinitializer)
	{
		FactorOrder factor = authnContext.getAuthenticationStepContext().factor; 
		return factor == FactorOrder.FIRST ? 
					authnProcessor.processFirstFactorResult(
							authnResult, 
							authnContext.getAuthenticationStepContext(), 
							authnContext.getInitialLoginMachine(), 
							authnContext.getAuthenticationTriggeringContext().rememberMeSet,
							httpRequest, 
							httpResponse,
							sessionReinitializer) :
					authnProcessor.processSecondFactorResult(
							authnContext.getAuthenticationTriggeringContext().firstFactorAuthnState,
							authnResult, 
							authnContext.getAuthenticationStepContext(), 
							authnContext.getInitialLoginMachine(), 
							authnContext.getAuthenticationTriggeringContext().rememberMeSet,
							httpRequest, 
							httpResponse,
							sessionReinitializer);
	}
	
	private PostAuthenticationStepDecision processSandboxAuthenticationResult(RedirectedAuthnState authnContext, 
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
