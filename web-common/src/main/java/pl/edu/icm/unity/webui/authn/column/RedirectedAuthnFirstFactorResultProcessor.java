/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.webui.authn.AccessBlockedDialog;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.NotificationPopup;

class RedirectedAuthnFirstFactorResultProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, RedirectedAuthnFirstFactorResultProcessor.class);
	private final MessageSource msg;
	private final WebAuthenticationProcessor authnProcessor;
	private final Supplier<Boolean> rememberMeProvider;
	private final ExecutorsService execService;
	private final Function<RemoteAuthenticationResult, UnknownUserDialog> unknownUserDialogProvider; 
	
	RedirectedAuthnFirstFactorResultProcessor(MessageSource msg, WebAuthenticationProcessor authnProcessor,
			Supplier<Boolean> rememberMeProvider, ExecutorsService execService,
			Function<RemoteAuthenticationResult, UnknownUserDialog> unknownUserDialogProvider)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.rememberMeProvider = rememberMeProvider;
		this.execService = execService;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
	}

	Optional<PartialAuthnState> onCompletedAuthentication(RemoteAuthenticationResult result, 
			AuthenticationStepContext stepContext)
	{
		String clientIp = HTTPRequestContext.getCurrent().getClientIP();
		log.trace("Received authentication result of the primary authenticator " + result);
		try
		{
			return authnProcessor.processPrimaryAuthnResult(
					result, 
					clientIp, 
					stepContext.realm, 
					stepContext.selectedAuthnFlow, 
					rememberMeProvider.get(), 
					stepContext.authnOptionId);
		} catch (UnknownRemoteUserException e)
		{
			handleUnknownUser(e, clientIp);
		} catch (AuthenticationException e)
		{
			log.trace("Authentication failed ", e);
			String originalError = result.getStatus() == Status.deny ? result.getErrorResult().error.resovle(msg) : null;
			handleError(msg.getMessage(e.getMessage()), originalError, clientIp);
		}
		return Optional.empty();
	}
	
	private void handleUnknownUser(UnknownRemoteUserException e, String clientIp)
	{
		if (e.getFormForUser() != null || e.getResult().getUnknownRemotePrincipalResult().enableAssociation)
		{
			log.trace("Authentication successful, user unknown, showing unknown user dialog");
			showUnknownUserDialog(e);
		} else
		{
			log.trace("Authentication successful, user unknown, no registration form");
			handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"), null, clientIp);
		}
	}
	
	private void handleError(String genericError, String authenticatorError, String clientIp)
	{
		//authNPanel.focusIfPossible(); TODO
		String errorToShow = authenticatorError == null ? genericError : authenticatorError;
		NotificationPopup.showError(errorToShow, "");
		showWaitScreenIfNeeded(clientIp);
	}
	
	private void showWaitScreenIfNeeded(String clientIp)
	{
		UnsuccessfulAuthenticationCounter counter = StandardWebAuthenticationProcessor.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
		{
			AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
			dialog.show();
			return;
		}
	}
	
	private void showUnknownUserDialog(UnknownRemoteUserException ee)
	{
		UnknownUserDialog dialog = unknownUserDialogProvider.apply(ee.getResult()); 
		dialog.show();
	}
}
