/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.sandbox;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import io.imunity.vaadin.auth.ColumnInstantAuthenticationScreen;
import io.imunity.vaadin.endpoint.common.LoginMachineDetailsExtractor;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Collects authN result from the first authenticator of the selected flow
 * and process it in sandbox way in case of not redirected logins.
 * Proper setup of contexts, remember me unsupported,
 * unknown remote user in the sandbox case equivalent to successful result.
 */
class FirstFactorSandboxAuthnCallback implements VaadinAuthentication.AuthenticationCallback
{
	private static final Duration DELAY_WINDOW_CLOSING_AFTER_ERROR_FOR = Duration.ofSeconds(5);
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, FirstFactorSandboxAuthnCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final ColumnInstantAuthenticationScreen.FirstFactorAuthenticationListener authNListener;
	private final AuthenticationStepContext stepContext;
	private final SandboxAuthnRouter sandboxRouter;
	private final NotificationPresenter notificationPresenter;

	FirstFactorSandboxAuthnCallback(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor,
			AuthenticationStepContext stepContext,
			SandboxAuthnRouter sandboxRouter,
			ColumnInstantAuthenticationScreen.FirstFactorAuthenticationListener authNListener,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.stepContext = stepContext;
		this.authNListener = authNListener;
		this.sandboxRouter = sandboxRouter;
		this.notificationPresenter = notificationPresenter;
		checkNotNull(sandboxRouter);
	}


	@Override
	public void onCompletedAuthentication(AuthenticationResult result, AuthenticationRetrievalContext retrievalContext)
	{
		log.trace("Received sandbox authentication result of the primary authenticator " + result);
		VaadinServletRequest servletRequest = VaadinServletRequest.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		SandboxAuthenticationResult sandboxAuthnResult = SandboxAuthenticationResult.getInstanceFromResult(result);
		PostAuthenticationStepDecision postFirstFactorDecision = authnProcessor.processFirstFactorSandboxAuthnResult(
				sandboxAuthnResult, stepContext, loginMachineDetails, servletRequest, sandboxRouter);
		switch (postFirstFactorDecision.getDecision())
		{
			case COMPLETED ->
			{
				log.trace("Authentication completed");
				closeWindow();
			}
			case ERROR ->
			{
				log.trace("Authentication failed ");
				handleError(postFirstFactorDecision.getErrorDetail().error.resovle(msg));
			}
			case GO_TO_2ND_FACTOR ->
			{
				log.trace("Authentication requires 2nd factor");
				switchToSecondaryAuthentication(postFirstFactorDecision.getSecondFactorDetail().postFirstFactorResult);
			}
			case UNKNOWN_REMOTE_USER ->
			{
				log.trace("Authentication resulted in unknown remote user");
				handleUnknownUser(postFirstFactorDecision.getUnknownRemoteUserDetail().unknownRemotePrincipal);
			}
			default ->
					throw new IllegalStateException("Unknown authn decision: " + postFirstFactorDecision.getDecision());
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
		return AuthenticationTriggeringContext.sandboxTriggeredFirstFactor(sandboxRouter);
	}

	/**
	 * Clears the UI so a new authentication can be started.
	 */
	private void setAuthenticationAborted()
	{
		if (authNListener != null)
			authNListener.authenticationAborted();
	}

	private void closeWindow()
	{
		UI.getCurrent().getPage().executeJs("window.close();");
	}

	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		if (authNListener != null)
			authNListener.switchTo2ndFactor(partialState);
	}


	private void handleError(String errorToShow)
	{
		setAuthenticationAborted();
		notificationPresenter.showError(errorToShow, "");
		scheduleWindowClose();
	}

	private void handleUnknownUser(UnknownRemotePrincipalResult result)
	{
		closeWindow();
	}

	private void scheduleWindowClose()
	{
		UI ui = UI.getCurrent();
		new Thread(() ->
		{
			try
			{
				Thread.sleep(DELAY_WINDOW_CLOSING_AFTER_ERROR_FOR.toMillis());
			} catch (InterruptedException e)
			{
			}

			ui.access(this::closeWindow);
		}).start();
	}
}
