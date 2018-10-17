/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.JavaScript;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;

/**
 * Specialized implementation of web authn processor: skips session creation and most of the application part of
 * authentication results.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
class SandboxAuthenticationProcessor implements WebAuthenticationProcessor
{
	@Autowired
	private AuthenticationProcessor authnProcessor;

	private SandboxAuthnRouter sandboxRouter;
	
	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter)
	{
		this.sandboxRouter = sandboxRouter;
	}

	@Override
	public Optional<PartialAuthnState> processPrimaryAuthnResult(AuthenticationResult result, String clientIp, 
			AuthenticationRealm realm,
			AuthenticationFlow authenticationFlow, boolean rememberMe, String authnOptionId) throws AuthenticationException
	{
		UnsuccessfulAuthenticationCounter counter = StandardWebAuthenticationProcessor.getLoginCounter();
		PartialAuthnState authnState;
		try
		{
			authnState = authnProcessor.processPrimaryAuthnResult(result, authenticationFlow, null);
		} catch (AuthenticationException e)
		{
			if (!(e instanceof UnknownRemoteUserException))
				counter.unsuccessfulAttempt(clientIp);
			throw e;
		}

		if (authnState.isSecondaryAuthenticationRequired())
			return Optional.ofNullable(authnState);
		
		AuthenticatedEntity logInfo = authnProcessor.finalizeAfterPrimaryAuthentication(authnState, false);

		finalizeLogin(logInfo);
		return Optional.empty();
	}

	@Override
	public void processSecondaryAuthnResult(PartialAuthnState state, AuthenticationResult result2, String clientIp, 
			AuthenticationRealm realm,
			AuthenticationFlow authenticationFlow, boolean rememberMe, String authnOptionId) throws AuthenticationException
	{
		UnsuccessfulAuthenticationCounter counter = StandardWebAuthenticationProcessor.getLoginCounter();
		AuthenticatedEntity logInfo;
		try
		{
			logInfo = authnProcessor.finalizeAfterSecondaryAuthentication(state, result2);
		} catch (AuthenticationException e)
		{
			if (!(e instanceof UnknownRemoteUserException))
				counter.unsuccessfulAttempt(clientIp);
			throw e;
		}

		finalizeLogin(logInfo);
	}
	
	private void finalizeLogin(AuthenticatedEntity logInfo) throws AuthenticationException
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
	public void logout()
	{
	}

	@Override
	public void logout(boolean soft)
	{
	}
}
