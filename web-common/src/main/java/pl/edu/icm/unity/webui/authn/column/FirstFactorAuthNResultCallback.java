/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
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
	private final UnityMessageSource msg;
	private final WebAuthenticationProcessor authnProcessor;
	private final AuthenticationRealm realm;
	private final AuthenticationFlow selectedAuthnFlow;
	private final Supplier<Boolean> rememberMeProvider;
	private final AuthenticationListener authNListener;
	private final String authnId;
	private final String endpointPath;
	private final FirstFactorAuthNPanel authNPanel;

	private String clientIp;
	
	public FirstFactorAuthNResultCallback(UnityMessageSource msg,
			WebAuthenticationProcessor authnProcessor, AuthenticationRealm realm,
			AuthenticationFlow selectedAuthnFlow, Supplier<Boolean> rememberMeProvider,
			AuthenticationListener authNListener, String authnId, String endpointPath,
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
		log.trace("Received authentication result of the primary authenticator " + result);
		try
		{
			Optional<PartialAuthnState> partialState = authnProcessor.processPrimaryAuthnResult(
					result, clientIp, realm, 
					selectedAuthnFlow, rememberMeProvider.get(), authnId);
			if (!partialState.isPresent())
			{
				setAuthenticationCompleted();
			} else
			{
				switchToSecondaryAuthentication(partialState.get());
			}
		} catch (UnknownRemoteUserException e)
		{
			handleUnknownUser(e);
		} catch (AuthenticationException e)
		{
			log.trace("Authentication failed ", e);
			handleError(msg.getMessage(e.getMessage()), error);
		}
	}

	@Override
	public void onStartedAuthentication(AuthenticationStyle style)
	{
		clientIp = HTTPRequestContext.getCurrent().getClientIP();
		if (authNListener != null)
			authNListener.authenticationStarted(style == AuthenticationStyle.WITH_EXTERNAL_CANCEL);
		setLastIdpCookie(authnId);
	}

	@Override
	public void onCancelledAuthentication()
	{
		setAuthenticationAborted();
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
	
	
	private void handleError(String genericError, String authenticatorError)
	{
		setAuthenticationAborted();
		authNPanel.focusIfPossible();
		String errorToShow = authenticatorError == null ? genericError : authenticatorError;
		NotificationPopup.showError(errorToShow, "");
		authNPanel.showWaitScreenIfNeeded(clientIp);
	}
	
	private void handleUnknownUser(UnknownRemoteUserException e)
	{
		if (e.getFormForUser() != null || e.getResult().isEnableAssociation())
		{
			log.trace("Authentication successful, user unknown, "
					+ "showing unknown user dialog");
			setAuthenticationAborted();
			authNPanel.showUnknownUserDialog(e);
		} else
		{
			log.trace("Authentication successful, user unknown, "
					+ "no registration form");
			handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"), null);
		}
	}
	
	private void setLastIdpCookie(String idpKey)
	{
		if (endpointPath == null)
			return;
		VaadinResponse resp = VaadinService.getCurrentResponse();
		resp.addCookie(PreferredAuthenticationHelper.createLastIdpCookie(endpointPath, idpKey));
	}
	
	/**
	 * Used by upstream code holding this component to be informed about changes in this component. 
	 */
	interface AuthenticationListener
	{
		void authenticationStarted(boolean showProgress);
		void authenticationAborted();
		void authenticationCompleted();
		void switchTo2ndFactor(PartialAuthnState partialState);
	}
}
