/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.LoginMachineDetailsExtractor;
import org.apache.logging.log4j.Logger;
import org.vaadin.firitin.util.WebStorage;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;

import java.util.function.Supplier;

/**
 * Collects authN result from the first authenticator of the selected flow
 * and process it: manages state of the rest of the UI (cancel button, notifications, registration) 
 * and if needed proceeds to 2nd authenticator.
 * 
 * Responsible only for processing results in cases when authentication was not using redirect.
 */
class FirstFactorAuthNResultCallback implements VaadinAuthentication.AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, FirstFactorAuthNResultCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final Supplier<Boolean> rememberMeProvider;
	private final ColumnInstantAuthenticationScreen.FirstFactorAuthenticationListener authNListener;
	private final NotificationPresenter notificationPresenter;
	private final FirstFactorAuthNPanel authNPanel;
	private final AuthenticationStepContext stepContext;

	public FirstFactorAuthNResultCallback(MessageSource msg,
	                                      InteractiveAuthenticationProcessor authnProcessor,
	                                      AuthenticationStepContext stepContext,
	                                      Supplier<Boolean> rememberMeProvider,
	                                      ColumnInstantAuthenticationScreen.FirstFactorAuthenticationListener authNListener,
	                                      FirstFactorAuthNPanel authNPanel,
	                                      NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.stepContext = stepContext;
		this.rememberMeProvider = rememberMeProvider;
		this.authNListener = authNListener;
		this.authNPanel = authNPanel;
		this.notificationPresenter = notificationPresenter;
	}


	@Override
	public void onCompletedAuthentication(AuthenticationResult result, AuthenticationRetrievalContext authenticationRetrievalContext)
	{
		log.trace("Received authentication result of the primary authenticator " + result);
		VaadinServletRequest servletRequest = VaadinServletRequest.getCurrent();
		VaadinServletResponse servletResponse = VaadinServletResponse.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		PostAuthenticationStepDecision postFirstFactorDecision = authnProcessor.processFirstFactorResult(result,
				stepContext, loginMachineDetails, rememberMeProvider.get(), servletRequest, servletResponse,
				new VaadinSessionReinitializer());
		switch (postFirstFactorDecision.getDecision())
		{
		case COMPLETED:
			log.trace("Authentication completed");
			setAuthenticationCompleted();
			return;
		case ERROR:
			log.trace("Authentication failed ");
			handleError(postFirstFactorDecision.getErrorDetail().error.resovle(msg));
			return;
		case GO_TO_2ND_FACTOR:
			log.trace("Authentication requires 2nd factor");
			switchToSecondaryAuthentication(postFirstFactorDecision.getSecondFactorDetail().postFirstFactorResult);
			return;
		case UNKNOWN_REMOTE_USER:
			log.trace("Authentication resulted in unknown remote user");
			handleUnknownUser(postFirstFactorDecision.getUnknownRemoteUserDetail().unknownRemotePrincipal);
			return;
		default:
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
		return AuthenticationTriggeringContext.authenticationTriggeredFirstFactor(rememberMeProvider.get());
	}
	
	/**
	 * Clears the UI so a new authentication can be started.
	 */
	private void setAuthenticationAborted()
	{
		if (authNListener != null)
			authNListener.authenticationAborted();
	}

	private void setAuthenticationCompleted()
	{
		if (authNListener != null)
			authNListener.authenticationCompleted();
		UI ui = UI.getCurrent();
		if (ui == null)
		{
			log.error("BUG Can't get UI to redirect the authenticated user.");
			throw new IllegalStateException("AuthenticationProcessor.authnInternalError");
		}
		WebStorage.getItem(
				WebStorage.Storage.sessionStorage,
				"redirect-url",
				value -> UI.getCurrent().getPage().setLocation(value)
		);
	}
	
	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		if (authNListener != null)
			authNListener.switchTo2ndFactor(partialState);
	}
	
	
	private void handleError(String errorToShow)
	{
		setAuthenticationAborted();
		authNPanel.focusIfPossible();
		notificationPresenter.showError(errorToShow, "");
	}
	
	private void handleUnknownUser(UnknownRemotePrincipalResult result)
	{
		if (result.formForUnknownPrincipal != null || result.enableAssociation)
		{
			log.trace("Authentication successful, user unknown, showing unknown user dialog");
			setAuthenticationAborted();
			authNPanel.showUnknownUserDialog(result);
		} else
		{
			log.trace("Authentication successful, user unknown, no registration form");
			handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"));
		}
	}
}
