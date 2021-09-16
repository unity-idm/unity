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
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen.SecondFactorAuthenticationListener;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Collects authN results from the 2nd authenticator. Afterwards, the final authentication result 
 * processing is launched.
 */
class SecondFactorAuthNResultCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SecondFactorAuthNResultCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final SecondFactorAuthenticationListener authNListener;
	private final Supplier<Boolean> rememberMeProvider;
	private final PartialAuthnState partialState;
	private final SecondFactorAuthNPanel authNPanel;
	private final AuthenticationStepContext stepContext;

	SecondFactorAuthNResultCallback(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor,
			AuthenticationStepContext stepContext,
			SecondFactorAuthenticationListener authNListener,
			Supplier<Boolean> rememberMeProvider,
			PartialAuthnState partialState,
			SecondFactorAuthNPanel authNPanel)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.stepContext = stepContext;
		this.authNListener = authNListener;
		this.rememberMeProvider = rememberMeProvider;
		this.partialState = partialState;
		this.authNPanel = authNPanel;
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
		processAuthn(result);
	}
	
	private void processAuthn(AuthenticationResult result)
	{
		log.trace("Received authentication result of the 2nd authenticator" + result);
		VaadinServletRequest servletRequest = VaadinServletRequest.getCurrent();
		VaadinServletResponse servletResponse = VaadinServletResponse.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		PostAuthenticationStepDecision postSecondFactorDecision = authnProcessor.processSecondFactorResult(partialState,
				result, stepContext, loginMachineDetails, rememberMeProvider.get(), servletRequest, servletResponse,
				new VaadinSessionReinitializer());
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
		return AuthenticationTriggeringContext.authenticationTriggeredSecondFactor(rememberMeProvider.get(), 
				partialState);
	}
	
	private void handleError(String errorToShow)
	{
		log.info("Authentication failed {}", errorToShow);
		setAuthenticationAborted();
		authNPanel.focusIfPossible();
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
		UI ui = UI.getCurrent();
		if (ui == null)
		{
			log.error("BUG Can't get UI to redirect the authenticated user.");
			throw new IllegalStateException("AuthenticationProcessor.authnInternalError");
		}
		ui.getPage().reload();
	}
}