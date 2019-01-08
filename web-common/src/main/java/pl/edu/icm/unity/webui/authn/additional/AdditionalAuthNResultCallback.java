/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.additional;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler.AuthnResult;

/**
 * Collects authN result from an authenticator and merely forwards a simplified information to the holding 
 * code: whether authentication was successful, canceled or failed. 
 * 
 * @author K. Benedyczak
 */
class AdditionalAuthNResultCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdditionalAuthNResultCallback.class);
	private final Consumer<AuthnResult> resultConsumer;
	private final SessionManagement sessionMan;
	private final String authenticatorId;

	public AdditionalAuthNResultCallback(SessionManagement sessionMan, String authenticatorId, 
			Consumer<AuthnResult> resultConsumer)
	{
		this.sessionMan = sessionMan;
		this.authenticatorId = authenticatorId;
		this.resultConsumer = resultConsumer;
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
		log.trace("Received authentication result of the additional authentication {}", result);
		if (result.getStatus() == Status.success)
		{
			String sessionId = InvocationContext.getCurrent().getLoginSession().getId();
			sessionMan.recordAdditionalAuthentication(sessionId, authenticatorId);
			updateLoginSessionInHttpSession();
		}
		
		resultConsumer.accept(result.getStatus() == Status.success? AuthnResult.SUCCESS : AuthnResult.ERROR);
	}

	private void updateLoginSessionInHttpSession()
	{
		LoginSession updatedSession = InvocationContext.getCurrent().getLoginSession();
		VaadinSession vSession = VaadinSession.getCurrent();
		if (vSession != null)
			vSession.getSession().setAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY, updatedSession);
	}
	
	@Override
	public void onFailedAuthentication(AuthenticationResult result, String error,
			Optional<String> errorDetail)
	{
		log.trace("Received authentication result of the additional authentication {}", result);
		resultConsumer.accept(AuthnResult.ERROR);
	}
	
	@Override
	public void onStartedAuthentication(AuthenticationStyle style)
	{
	}

	@Override
	public void onCancelledAuthentication()
	{
		resultConsumer.accept(AuthnResult.CANCEL);
	}
}
