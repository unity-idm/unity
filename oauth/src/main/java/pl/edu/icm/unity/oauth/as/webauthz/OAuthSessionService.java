/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.webui.LoginInProgressService;
import pl.edu.icm.unity.webui.LoginInProgressService.SignInContextKey;
import pl.edu.icm.unity.webui.LoginInProgressService.SignInContextSession;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;

/**
 * Provides unified API to perform OAuth session bootstrap and cleanup.
 */
@Component
class OAuthSessionService
{
	/**
	 * Under this key the OAuthContext object is stored in the session.
	 */
	private static final String SESSION_OAUTH_CONTEXT = "oauth2AuthnContextKey";
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_OAUTH, OAuthSessionService.class);
	private static final LoginInProgressService<OAuthAuthzContext> LOGIN_IN_PROGRESS_SERVICE = new LoginInProgressService<>(
			SESSION_OAUTH_CONTEXT);
	static final String URL_PARAM_CONTEXT_KEY = LoginInProgressService.URL_PARAM_CONTEXT_KEY;

	private final SessionManagement sessionMan;
	
	OAuthSessionService(SessionManagement sessionMan)
	{
		this.sessionMan = sessionMan;
	}

	static SignInContextKey setContext(HttpSession session, OAuthAuthzContext context)
	{
		return LOGIN_IN_PROGRESS_SERVICE.setContext(session, context);
	}
	
	static Optional<OAuthAuthzContext> getContext(HttpServletRequest req)
	{
		return LOGIN_IN_PROGRESS_SERVICE.getContext(req);
	}

	static OAuthAuthzContext getVaadinContext()
	{
		return LOGIN_IN_PROGRESS_SERVICE.getVaadinContext();
	}

	static boolean hasVaadinContext()
	{
		return LOGIN_IN_PROGRESS_SERVICE.hasVaadinContext();
	}
	
	void cleanupComplete(Optional<SignInContextSession> session, boolean invalidateSSOSession)
	{
		cleanupBeforeResponseSent(session);
		cleanupAfterResponseSent(session, invalidateSSOSession);
	}
	
	void cleanupBeforeResponseSent(Optional<SignInContextSession> session)
	{
		session.ifPresent(ses -> cleanupBeforeResponseSent(ses));
	}
	
	void cleanupAfterResponseSent(Optional<SignInContextSession> session, boolean invalidateSSOSession)
	{
		cleanupAfterResponseSent(session.orElse(null), invalidateSSOSession);
	}
	
	void cleanupBeforeResponseSent(SignInContextSession session)
	{
		LOG.trace("Cleaning OAuth session auto-proxy state");
		session.removeAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED);
	}
	
	void cleanupAfterResponseSent(SignInContextSession session, boolean invalidateSSOSession)
	{
		LOG.trace("Cleaning OAuth session (sso logout={})", invalidateSSOSession);
		LOGIN_IN_PROGRESS_SERVICE.cleanUpSignInContextAttribute(session);
		if (session != null)
		{
			session.removeAttribute(AuthorizationResponse.class.getName());
		}
		if (invalidateSSOSession)
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			sessionMan.removeSession(loginSession.getId(), true);
		}
	}
}
