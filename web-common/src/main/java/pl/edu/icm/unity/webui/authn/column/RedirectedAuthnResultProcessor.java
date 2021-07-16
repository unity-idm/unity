/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.webui.authn.AccessBlockedDialog;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.authn.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

class RedirectedAuthnResultProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, RedirectedAuthnResultProcessor.class);
	private final MessageSource msg;
	private final ExecutorsService execService;
	private final Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider;
	private final Consumer<PartialAuthnState> switchUITo2ndFactor; 
	
	RedirectedAuthnResultProcessor(MessageSource msg, ExecutorsService execService,
			Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider,
			Consumer<PartialAuthnState> switchUITo2ndFactor)
	{
		this.msg = msg;
		this.execService = execService;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.switchUITo2ndFactor = switchUITo2ndFactor;
	}

	void onCompletedAuthentication(PostAuthenticationStepDecision postAuthnStepDecision)
	{
		String clientIp = HTTPRequestContext.getCurrent().getClientIP();
		switch (postAuthnStepDecision.getDecision())
		{
		case COMPLETED:
			log.trace("Authentication completed");
			return;
		case ERROR:
			log.trace("Authentication failed ");
			handleError(postAuthnStepDecision.getErrorDetail().error.resovle(msg), clientIp);
			return;
		case GO_TO_2ND_FACTOR:
			log.trace("Authentication requires 2nd factor");
			switchUITo2ndFactor.accept(postAuthnStepDecision.getSecondFactorDetail().postFirstFactorResult);
			return;
		case UNKNOWN_REMOTE_USER:
			log.trace("Authentication resulted in unknown remote user");
			handleUnknownUser(postAuthnStepDecision.getUnknownRemoteUserDetail().unknownRemotePrincipal, clientIp);
			return;
		default:
			throw new IllegalStateException("Unknown authn decision: " + postAuthnStepDecision.getDecision());
		}
	}
	
	private void handleUnknownUser(UnknownRemotePrincipalResult result, String clientIp)
	{
		if (result.formForUnknownPrincipal != null || result.enableAssociation)
		{
			log.trace("Authentication successful, user unknown, showing unknown user dialog");
			showUnknownUserDialog(result);
		} else
		{
			log.trace("Authentication successful, user unknown, no registration form");
			handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"), clientIp);
		}
	}
	
	private void handleError(String authenticatorError, String clientIp)
	{
		NotificationPopup.showError(authenticatorError, "");
		showWaitScreenIfNeeded(clientIp);
	}
	
	private void showWaitScreenIfNeeded(String clientIp)
	{
		UnsuccessfulAuthenticationCounter counter = StandardWebLogoutHandler.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
		{
			AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
			dialog.show();
			return;
		}
	}
	
	private void showUnknownUserDialog(UnknownRemotePrincipalResult result)
	{
		UnknownUserDialog dialog = unknownUserDialogProvider.apply(result); 
		dialog.show();
	}
}
