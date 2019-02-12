/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.column.AuthNOption;

/**
 * Controls the sign up authentication flow and notifies the application through
 * {@link SignUpAuthNControllerListener} about key events that may happen during
 * sign up process.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class SignUpAuthNController
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SignUpAuthNController.class);
	private AuthenticationProcessor authnProcessor;
	private SignUpAuthNControllerListener listener;
	
	private AuthNOption selectedAuthNOption;
	
	public SignUpAuthNController(AuthenticationProcessor authnProcessor, SignUpAuthNControllerListener listener)
	{
		this.authnProcessor = authnProcessor;
		this.listener = listener;
	}

	public AuthenticationCallback buildCallback(AuthNOption option)
	{
		return new SignUpAuthnCallback(option);
	}
	
	private void processAuthn(AuthenticationResult result, String error)
	{
		LOG.info("processAuthn {}, {}", result, error);
		try
		{
			authnProcessor.processPrimaryAuthnResult(result, selectedAuthNOption.flow, 
					selectedAuthNOption.authenticatorUI.getId());
			resetSelectedAuthNOption();
			listener.onUserExists(result);
		} catch (AuthenticationException e)
		{
			if (e instanceof UnknownRemoteUserException)
			{
				listener.onUnknownUser(result);
			} else
			{
				listener.onAuthnError(e, error);
			}
		}
	}
	
	public void refresh(VaadinRequest request)
	{
		if (selectedAuthNOption != null)
			selectedAuthNOption.authenticatorUI.refresh(request);
	}

	public void manualCancel()
	{
		if (selectedAuthNOption != null)
		{
			selectedAuthNOption.authenticatorUI.clear();
			resetSelectedAuthNOption();
		}
	}
	
	private void resetSelectedAuthNOption()
	{
		selectedAuthNOption = null;
	}

	private class SignUpAuthnCallback implements AuthenticationCallback
	{
		private final AuthNOption authNOption;

		SignUpAuthnCallback(AuthNOption option)
		{
			this.authNOption = option;
		}

		@Override
		public void onStartedAuthentication(AuthenticationStyle authenticationStyle)
		{
			selectedAuthNOption = authNOption;
			listener.onAuthnStarted(authenticationStyle == AuthenticationStyle.WITH_EXTERNAL_CANCEL);
		}

		@Override
		public void onCompletedAuthentication(AuthenticationResult result)
		{
			processAuthn(result, null);
		}

		@Override
		public void onFailedAuthentication(AuthenticationResult result, String error, Optional<String> errorDetail)
		{
			processAuthn(result, error);
		}

		@Override
		public void onCancelledAuthentication()
		{
			resetSelectedAuthNOption();
			listener.onAuthnCancelled();
		}
	}
}
