/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.additional;

import com.vaadin.flow.server.VaadinSession;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticationRetrievalContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import io.imunity.vaadin.auth.VaadinAuthentication;

import java.util.function.Consumer;

/**
 * Collects authN result from an authenticator and merely forwards a simplified information to the holding 
 * code: whether authentication was successful, canceled or failed.
 */
class AdditionalAuthNResultCallback implements VaadinAuthentication.AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdditionalAuthNResultCallback.class);
	private final Consumer<AdditionalAuthnHandler.AuthnResult> resultConsumer;
	private final SessionManagement sessionMan;
	private final AuthenticationOptionKey authenticatorId;

	public AdditionalAuthNResultCallback(SessionManagement sessionMan, AuthenticationOptionKey authenticatorId, 
			Consumer<AdditionalAuthnHandler.AuthnResult> resultConsumer)
	{
		this.sessionMan = sessionMan;
		this.authenticatorId = authenticatorId;
		this.resultConsumer = resultConsumer;
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result, AuthenticationRetrievalContext retrivalContext)
	{
		log.trace("Received authentication result of the additional authentication {}", result);
		if (result.getStatus() == Status.success)
		{
			String sessionId = InvocationContext.getCurrent().getLoginSession().getId();
			sessionMan.recordAdditionalAuthentication(sessionId, authenticatorId);
			updateLoginSessionInHttpSession();
		}
		
		resultConsumer.accept(result.getStatus() == Status.success ? AdditionalAuthnHandler.AuthnResult.SUCCESS : AdditionalAuthnHandler.AuthnResult.ERROR);
	}

	private void updateLoginSessionInHttpSession()
	{
		LoginSession updatedSession = InvocationContext.getCurrent().getLoginSession();
		VaadinSession vSession = VaadinSession.getCurrent();
		if (vSession != null)
			vSession.getSession().setAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY, updatedSession);
	}
	
	@Override
	public void onStartedAuthentication()
	{
	}

	@Override
	public void onCancelledAuthentication()
	{
		resultConsumer.accept(AdditionalAuthnHandler.AuthnResult.CANCEL);
	}

	@Override
	public AuthenticationTriggeringContext getTriggeringContext()
	{
		return AuthenticationTriggeringContext.authenticationTriggeredFirstFactor();
	}
}
