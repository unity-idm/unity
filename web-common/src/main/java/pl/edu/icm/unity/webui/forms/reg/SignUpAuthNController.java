/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
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
	private final MessageSource msg;
	
	public SignUpAuthNController(AuthenticationProcessor authnProcessor, SignUpAuthNControllerListener listener, 
			MessageSource msg)
	{
		this.authnProcessor = authnProcessor;
		this.listener = listener;
		this.msg = msg;
	}

	public AuthenticationCallback buildCallback(AuthNOption option)
	{
		return new SignUpAuthnCallback(option);
	}
	
	private void processAuthn(AuthenticationResult result)
	{
		LOG.info("Processing results of remote authentication {}", result);
		if (LOG.isDebugEnabled())
			LOG.debug("Complete remote authn context:\n{}", result.toStringFull());
		try
		{
			authnProcessor.processPrimaryAuthnResult(result, selectedAuthNOption.flow, 
					new AuthenticationOptionKey(selectedAuthNOption.authenticator.getAuthenticatorId(),
							selectedAuthNOption.authenticatorUI.getId()));
			resetSelectedAuthNOption();
			listener.onUserExists(result);
		} catch (AuthenticationException e)
		{
			if (e instanceof UnknownRemoteUserException)
			{
				listener.onUnknownUser(result);
			} else
			{
				String originalError = result.getStatus() == Status.deny ? result.getErrorResult().error.resovle(msg) : null;
				listener.onAuthnError(e, originalError);
			}
		}
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
		public void onStartedAuthentication()
		{
			selectedAuthNOption = authNOption;
			listener.onAuthnStarted();
		}

		@Override
		public void onCompletedAuthentication(AuthenticationResult result)
		{
			processAuthn(result);
		}

		@Override
		public void onCancelledAuthentication()
		{
			resetSelectedAuthNOption();
			listener.onAuthnCancelled();
		}

		@Override
		public boolean isSetRememberMe()
		{
			return false;
		}
		
		@Override
		public PartialAuthnState getPostFirstFactorAuthnState()
		{
			return null;
		}
	}
}
