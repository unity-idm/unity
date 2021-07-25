/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import static com.google.common.base.Preconditions.checkNotNull;

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
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen.FirstFactorAuthenticationListener;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Collects authN result from the first authenticator of the selected flow
 * and process it in sandbox way in case of not redirected logins. 
 * Proper setup of contexts, remember me unsupported, 
 * unknown remote user in the sandbox case equivalent to successful result.
 */
class FirstFactorSandboxAuthnCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, FirstFactorSandboxAuthnCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final FirstFactorAuthenticationListener authNListener;
	private final AuthenticationStepContext stepContext;
	private final SandboxAuthnRouter sandboxRouter;
	
	FirstFactorSandboxAuthnCallback(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor,
			AuthenticationStepContext stepContext,
			SandboxAuthnRouter sandboxRouter,
			FirstFactorAuthenticationListener authNListener) 
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.stepContext = stepContext;
		this.authNListener = authNListener;
		this.sandboxRouter = sandboxRouter;
		checkNotNull(sandboxRouter);
	}


	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
		log.trace("Received sandbox authentication result of the primary authenticator " + result);
		VaadinServletRequest servletRequest = VaadinServletRequest.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		PostAuthenticationStepDecision postFirstFactorDecision = authnProcessor.processFirstFactorSandboxAuthnResult(
				result, 
				stepContext, loginMachineDetails, servletRequest, sandboxRouter);
		switch (postFirstFactorDecision.getDecision())
		{
		case COMPLETED:
			log.trace("Authentication completed");
			closeWindow();
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
		JavaScript.getCurrent().execute("window.close();");
	}
	
	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		if (authNListener != null)
			authNListener.switchTo2ndFactor(partialState);
	}
	
	
	private void handleError(String errorToShow)
	{
		setAuthenticationAborted();
		NotificationPopup.showError(errorToShow, "");
	}
	
	private void handleUnknownUser(UnknownRemotePrincipalResult result)
	{
		closeWindow();
	}
}
