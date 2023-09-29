/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.sandbox;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import io.imunity.vaadin.auth.ColumnInstantAuthenticationScreen;
import io.imunity.vaadin.endpoint.common.LoginMachineDetailsExtractor;
import io.imunity.vaadin.auth.UnsuccessfulAuthenticationHelper;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

/**
 * Collects authN results from the 2nd authenticator. Afterwards, the final authentication result 
 * processing is launched.
 */
class SecondFactorSandboxAuthnCallback implements VaadinAuthentication.AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SecondFactorSandboxAuthnCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final ColumnInstantAuthenticationScreen.SecondFactorAuthenticationListener authNListener;
	private final PartialAuthnState partialState;
	private final SandboxAuthnRouter sandboxRouter;
	private final AuthenticationStepContext stepContext;
	private final NotificationPresenter notificationPresenter;

	SecondFactorSandboxAuthnCallback(MessageSource msg,
	                                 InteractiveAuthenticationProcessor authnProcessor,
	                                 AuthenticationStepContext stepContext,
	                                 ColumnInstantAuthenticationScreen.SecondFactorAuthenticationListener authNListener, SandboxAuthnRouter sandboxRouter,
	                                 PartialAuthnState partialState,
	                                 NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.stepContext = stepContext;
		this.authNListener = authNListener;
		this.sandboxRouter = sandboxRouter;
		this.partialState = partialState;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result, AuthenticationRetrievalContext retrievalContext)
	{
		processAuthn(result, retrievalContext);
	}
	
	private void processAuthn(AuthenticationResult result, AuthenticationRetrievalContext retrievalContext)
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
			case COMPLETED ->
			{
				log.debug("Authentication completed");
				setAuthenticationCompleted();
			}
			case ERROR ->
			{
				handleError(postSecondFactorDecision.getErrorDetail().error.resovle(msg));
				if (!retrievalContext.supportOnlySecondFactorReseting || UnsuccessfulAuthenticationHelper.failedAttemptsExceeded())
				{
					switchToPrimaryAuthentication();
				}
			}
			case GO_TO_2ND_FACTOR ->
			{
				log.error("2nd factor required after 2nd factor? {}", result);
				throw new IllegalStateException("authentication error");
			}
			case UNKNOWN_REMOTE_USER ->
			{
				log.error("unknown remote user after 2nd factor? {}", result);
				throw new IllegalStateException("authentication error");
			}
			default ->
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
		notificationPresenter.showError(errorToShow, "");
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
		UI.getCurrent().getPage().executeJs("window.close();");
	}
}