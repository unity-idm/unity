/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Collects authN results from the 2nd authenticator. Afterwards, the final authentication result 
 * processing is launched.
 */
class SecondFactorAuthNResultCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SecondFactorAuthNResultCallback.class);
	private final UnityMessageSource msg;
	private final WebAuthenticationProcessor authnProcessor;
	private final AuthenticationRealm realm;
	private final AuthenticationListener authNListener;
	private final Supplier<Boolean> rememberMeProvider;
	private final PartialAuthnState partialState;
	private final SecondFactorAuthNPanel authNPanel;

	private String clientIp;


	SecondFactorAuthNResultCallback(UnityMessageSource msg,
			WebAuthenticationProcessor authnProcessor, AuthenticationRealm realm,
			AuthenticationListener authNListener, Supplier<Boolean> rememberMeProvider,
			PartialAuthnState partialState,
			SecondFactorAuthNPanel authNPanel)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.realm = realm;
		this.authNListener = authNListener;
		this.rememberMeProvider = rememberMeProvider;
		this.partialState = partialState;
		this.authNPanel = authNPanel;
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
		processAuthn(result, null);
	}
	

	@Override
	public void onFailedAuthentication(AuthenticationResult result, String error,
			Optional<String> errorDetail)
	{
		processAuthn(result, error);
	}
	
	private void processAuthn(AuthenticationResult result, String error)
	{
		log.trace("Received authentication result of the 2nd authenticator" + result);
		try
		{
			authnProcessor.processSecondaryAuthnResult(partialState, result, clientIp, realm, 
					partialState.getAuthenticationFlow(), rememberMeProvider.get(), 
					authNPanel.getAuthenticationOptionId());
			setAuthenticationCompleted();
		} catch (AuthenticationException e)
		{
			log.trace("Secondary authentication failed ", e);
			handleError(msg.getMessage(e.getMessage()), null);
			switchToPrimaryAuthentication();
		}
	}
	
	@Override
	public void onStartedAuthentication(AuthenticationStyle style)
	{
		clientIp = HTTPRequestContext.getCurrent().getClientIP();
		if (authNListener != null)
			authNListener.authenticationStarted(style == AuthenticationStyle.WITH_EXTERNAL_CANCEL);
	}

	@Override
	public void onCancelledAuthentication()
	{
		setAuthenticationAborted();
	}
	
	private void handleError(String genericError, String authenticatorError)
	{
		setAuthenticationAborted();
		authNPanel.focusIfPossible();
		String errorToShow = authenticatorError == null ? genericError : authenticatorError;
		NotificationPopup.showError(errorToShow, "");
		authNPanel.showWaitScreenIfNeeded(clientIp);
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
	}

	/**
	 * Used by upstream code holding this component to be informed about changes in this component. 
	 */
	public interface AuthenticationListener
	{
		void authenticationStarted(boolean showProgress);
		void authenticationAborted();
		void authenticationCompleted();
		void switchBackToFirstFactor();
	}
}