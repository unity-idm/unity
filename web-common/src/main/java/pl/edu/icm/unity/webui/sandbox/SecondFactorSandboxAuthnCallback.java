/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinServletRequest;
import com.vaadin.ui.JavaScript;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen.SecondFactorAuthenticationListener;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Collects authN results from the 2nd authenticator. Afterwards, the final authentication result 
 * processing is launched.
 */
class SecondFactorSandboxAuthnCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SecondFactorSandboxAuthnCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final SecondFactorAuthenticationListener authNListener;
	private final PartialAuthnState partialState;
	private final SandboxAuthnRouter sandboxRouter;
	private final AuthenticationStepContext stepContext;

	SecondFactorSandboxAuthnCallback(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor, 
			AuthenticationStepContext stepContext,
			SecondFactorAuthenticationListener authNListener, SandboxAuthnRouter sandboxRouter,
			PartialAuthnState partialState)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.stepContext = stepContext;
		this.authNListener = authNListener;
		this.sandboxRouter = sandboxRouter;
		this.partialState = partialState;
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
		processAuthn(result);
	}
	
	private void processAuthn(AuthenticationResult result)
	{
		log.trace("Received sandbox authentication result of the 2nd authenticator" + result);
		VaadinServletRequest servletRequest = VaadinServletRequest.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		SandboxAuthenticationResult sandboxAuthnResult = SandboxAuthenticationResult.getInstanceFromResult(result);
		PostAuthenticationStepDecision postSecondFactorDecision = authnProcessor.processSecondFactorSandboxAuthnResult(
				partialState, sandboxAuthnResult, stepContext, 
				loginMachineDetails, servletRequest, sandboxRouter);
		switch (postSecondFactorDecision.getDecision())
		{
		case COMPLETED:
			log.debug("Authentication completed");
			setAuthenticationCompleted();
			return;
		case ERROR:
			handleError(postSecondFactorDecision.getErrorDetail().error.resovle(msg));
			switchToPrimaryAuthentication();
			return;
		case GO_TO_2ND_FACTOR:
			log.error("2nd factor required after 2nd factor? {}", result);
			throw new IllegalStateException("authentication error");
		case UNKNOWN_REMOTE_USER:
			log.error("unknown remote user after 2nd factor? {}", result);
			throw new IllegalStateException("authentication error");
		default:
			throw new IllegalStateException("Unknown authn decision: " + postSecondFactorDecision.getDecision());
		}
	}
	
	@Override
	public void onStartedAuthentication()
	{
		if (authNListener != null)
			authNListener.authenticationStarted();
	}

	@Override
	public void onCancelledAuthentication()
	{
		setAuthenticationAborted();
	}

	@Override
	public AuthenticationTriggeringContext getTriggeringContext()
	{
		return AuthenticationTriggeringContext.sandboxTriggeredSecondFactor(partialState, sandboxRouter);
	}
	
	private void handleError(String errorToShow)
	{
		log.info("Authentication failed {}", errorToShow);
		setAuthenticationAborted();
		NotificationPopup.showErrorAutoClosing(errorToShow, "");
	}
	
	/**
	 * Resets the authentication UI to the initial state
	 */
	private void switchToPrimaryAuthentication()
	{
		if (authNListener != null)
			authNListener.switchBackToFirstFactor();
	}
	
	private void setAuthenticationAborted()
	{
		if (authNListener != null)
			authNListener.authenticationAborted();
	}
	
	private void setAuthenticationCompleted()
	{
		if (authNListener != null)
			authNListener.authenticationCompleted();
		closeWindow();
	}
	
	private void closeWindow()
	{
		JavaScript.getCurrent().execute("window.close();");
	}
}