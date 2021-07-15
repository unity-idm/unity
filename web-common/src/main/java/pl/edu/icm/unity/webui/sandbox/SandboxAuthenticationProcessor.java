/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.JavaScript;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision.ErrorDetail;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;

/**
 * Specialized implementation of web authn processor: skips session creation and most of the application part of
 * authentication results.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
class SandboxAuthenticationProcessor implements InteractiveAuthenticationProcessor
{
	@Autowired
	private AuthenticationProcessor authnProcessor;

	private SandboxAuthnRouter sandboxRouter;
	
	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter)
	{
		this.sandboxRouter = sandboxRouter;
	}

	@Override
	public PostAuthenticationStepDecision processFirstFactorResult(AuthenticationResult result,
			AuthenticationStepContext stepContext, LoginMachineDetails machineDetails,
			boolean setRememberMe, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	{
		UnsuccessfulAuthenticationCounter counter = StandardWebLogoutHandler.getLoginCounter();
		PartialAuthnState authnState;
		try
		{
			authnState = authnProcessor.processPrimaryAuthnResult(result, stepContext.selectedAuthnFlow, null);
		} catch (AuthenticationException e)
		{
			if (!(e instanceof UnknownRemoteUserException))
				counter.unsuccessfulAttempt(machineDetails.getIp());
			return PostAuthenticationStepDecision.error(new ErrorDetail(new ResolvableError(e.getMessage())));
		}

		if (authnState.isSecondaryAuthenticationRequired())
			new IllegalStateException("Sandbox mode used flow which requires 2nd factor");
		
		AuthenticatedEntity logInfo = authnProcessor.finalizeAfterPrimaryAuthentication(authnState, false);

		finalizeLogin(logInfo);
		return PostAuthenticationStepDecision.completed();
	}

	@Override
	public PostAuthenticationStepDecision processSecondFactorResult(PartialAuthnState state,
			AuthenticationResult secondFactorResult, AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails, boolean setRememberMe, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse)
	{
		throw new UnsupportedOperationException("we are in sandbox mode");
	}

	@Override
	public void syntheticAuthenticate(AuthenticatedEntity authenticatedEntity,
			List<SessionParticipant> participants, AuthenticationOptionKey authnOptionKey,
			AuthenticationRealm realm, LoginMachineDetails machineDetails, boolean setRememberMe,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	{
		throw new UnsupportedOperationException("we are in sandbox mode");
	}
	
	private void finalizeLogin(AuthenticatedEntity logInfo)
	{
		if (logInfo != null && logInfo.getOutdatedCredentialId() != null)
		{
			//simply reload - we ensure that session reinit after login won't outdate session
			//authN handler anyway won't let us in to the target endpoint with outdated credential
			//and we will get outdated credential dialog from the AuthnUI
			UI ui = UI.getCurrent();
			ui.getPage().reload();
			return;
		}
		sandboxRouter.fireCompleteEvent(logInfo);
		JavaScript.getCurrent().execute("window.close();");
	}

	@Override
	public PostAuthenticationStepDecision processRemoteRegistrationResult(AuthenticationResult result,
			AuthenticationStepContext stepContext, LoginMachineDetails machineDetails,
			HttpServletRequest httpRequest)
	{
		throw new UnsupportedOperationException("we are in sandbox mode");
	}

}
