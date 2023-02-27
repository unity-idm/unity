/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationRetrievalContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen.FirstFactorAuthenticationListener;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Collects authN result from the first authenticator of the selected flow
 * and process it: manages state of the rest of the UI (cancel button, notifications, registration) 
 * and if needed proceeds to 2nd authenticator.
 * 
 * Responsible only for processing results in cases when authentication was not using redirect.
 */
class FirstFactorAuthNResultCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, FirstFactorAuthNResultCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final Supplier<Boolean> rememberMeProvider;
	private final FirstFactorAuthenticationListener authNListener;
	private final FirstFactorAuthNPanel authNPanel;
	private final AuthenticationStepContext stepContext;

	public FirstFactorAuthNResultCallback(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor,
			AuthenticationStepContext stepContext,
			Supplier<Boolean> rememberMeProvider,
			FirstFactorAuthenticationListener authNListener, 
			FirstFactorAuthNPanel authNPanel)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.stepContext = stepContext;
		this.rememberMeProvider = rememberMeProvider;
		this.authNListener = authNListener;
		this.authNPanel = authNPanel;
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
		ui.getPage().reload();
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
		NotificationPopup.showErrorAutoClosing(errorToShow, "");
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
