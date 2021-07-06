/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Collects authN result from the first authenticator of the selected flow
 * and process it: manages state of the rest of the UI (cancel button, notifications, registration) 
 * and if needed proceeds to 2nd authenticator. 
 * 
 * @author K. Benedyczak
 */
class FirstFactorAuthNResultCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			FirstFactorAuthNResultCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final AuthenticationRealm realm;
	private final AuthenticationFlow selectedAuthnFlow;
	private final Supplier<Boolean> rememberMeProvider;
	private final AuthenticationListener authNListener;
	private final AuthenticationOptionKey authnId;
	private final String endpointPath;
	private final FirstFactorAuthNPanel authNPanel;

	private String clientIp;
	
	public FirstFactorAuthNResultCallback(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor, 
			AuthenticationRealm realm,
			AuthenticationFlow selectedAuthnFlow, Supplier<Boolean> rememberMeProvider,
			AuthenticationListener authNListener, AuthenticationOptionKey authnId, String endpointPath,
			FirstFactorAuthNPanel authNPanel)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.realm = realm;
		this.selectedAuthnFlow = selectedAuthnFlow;
		this.rememberMeProvider = rememberMeProvider;
		this.authNListener = authNListener;
		this.authnId = authnId;
		this.endpointPath = endpointPath;
		this.authNPanel = authNPanel;
	}


	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
		log.trace("Received authentication result of the primary authenticator " + result);
		AuthenticationStepContext stepContext = new AuthenticationStepContext(realm, 
				selectedAuthnFlow, authnId, FactorOrder.FIRST, endpointPath);
		VaadinServletRequest servletRequest = VaadinServletRequest.getCurrent();
		VaadinServletResponse servletResponse = VaadinServletResponse.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		PostAuthenticationStepDecision postFirstFactorDecision = authnProcessor.processFirstFactorResult(
				result, 
				stepContext, loginMachineDetails, isSetRememberMe(), servletRequest, servletResponse);
		switch (postFirstFactorDecision.getDecision())
		{
		case COMPLETED:
			log.trace("Authentication completed");
			setAuthenticationCompleted();
			return;
		case ERROR:
			log.trace("Authentication failed ");
			handleError(postFirstFactorDecision.getErrorDetail().error.resovle(msg));
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
		clientIp = HTTPRequestContext.getCurrent().getClientIP();
		if (authNListener != null)
			authNListener.authenticationStarted();
	}

	@Override
	public void onCancelledAuthentication()
	{
		setAuthenticationAborted();
	}

	@Override
	public boolean isSetRememberMe()
	{
		return rememberMeProvider.get();
	}

	@Override
	public PartialAuthnState getPostFirstFactorAuthnState()
	{
		return null;
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
		NotificationPopup.showError(errorToShow, "");
		authNPanel.showWaitScreenIfNeeded(clientIp);
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
	
	/**
	 * Used by upstream code holding this component to be informed about changes in this component. 
	 */
	interface AuthenticationListener
	{
		void authenticationStarted();
		void authenticationAborted();
		void authenticationCompleted();
		void switchTo2ndFactor(PartialAuthnState partialState);
	}
}
