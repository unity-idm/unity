/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.server;

import io.imunity.vaadin.endpoint.common.EopException;
import org.apache.http.auth.BasicUserPrincipal;
import org.apache.log4j.MDC;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Request;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.CookieHelper;
import pl.edu.icm.unity.engine.api.utils.MDCKeys;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

/**
 * Servlet filter forwarding unauthenticated requests to the protected authentication servlet.
 * @author K. Benedyczak
 */
public class AuthenticationFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationFilter.class);

	private final String sessionCookieName;
	private final UnsuccessfulAuthenticationCounter dosGauard;
	private final SessionManagement sessionMan;
	private final LoginToHttpSessionBinder sessionBinder;
	private final RememberMeProcessor rememberMeHelper;
	private final AuthenticationRealm realm;
	private final NoSessionFilter noSessionFilter;
	
	public AuthenticationFilter(AuthenticationRealm realm,
	                            SessionManagement sessionMan, LoginToHttpSessionBinder sessionBinder, RememberMeProcessor rememberMeHelper)
	{
		this(realm, sessionMan, sessionBinder, rememberMeHelper, (req,resp) -> {});
	}
	
	
	public AuthenticationFilter(AuthenticationRealm realm,
	                            SessionManagement sessionMan, LoginToHttpSessionBinder sessionBinder, RememberMeProcessor rememberMeHelper,
	                            NoSessionFilter noSessionFilter)
	{
		//note: this is a separate counter to the main one which is stored as a servlet's attribute.
		// this is 'cos we need to separate regular net traffic (and not to block it - otherwise even 
		// notification about blocking woudn't show up). Still we need to prevent brute force attacks using 
		// fake session cookies - this object is responsible only for that.
		dosGauard = new DefaultUnsuccessfulAuthenticationCounter(realm.getBlockAfterUnsuccessfulLogins(), realm.getBlockFor()* 1000L);
		sessionCookieName = SessionCookie.getSessionCookieName(realm.getName());
		this.sessionMan = sessionMan;
		this.sessionBinder = sessionBinder;
		this.rememberMeHelper = rememberMeHelper;
		this.realm = realm;
		this.noSessionFilter = noSessionFilter;
	}
	
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		String clientIp = HTTPRequestContext.getCurrent().getClientIP();
	
		try 
		{
			handleForceLogin(httpRequest, httpResponse, chain);
			handleBoundSession(httpRequest, httpResponse, chain, clientIp);
			handleBlockedIP(httpResponse, clientIp);
			handleSessionFromCookie(httpRequest, httpResponse, chain, clientIp);
			handleRememberMe(httpRequest, httpResponse, chain, clientIp);
		
			log.error("Unprocessed request, should not happen, forward to authn for safety only");
			//it should not happen, for safety only  
			forwardtoAuthn(httpRequest, httpResponse, chain);
		} catch (EopException e)
		{

		} 
	}
	
	private void handleForceLogin(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, FilterChain chain) throws EopException, IOException, ServletException
	{
		AuthenticationPolicy policy = AuthenticationPolicy.getPolicy(httpRequest.getSession());
		if (policy.equals(AuthenticationPolicy.FORCE_LOGIN))
		{
			log.trace("Force reauthentication");
			forwardtoAuthn(httpRequest, httpResponse, chain);
			throw new EopException();
		}
	}

	private void handleBoundSession(HttpServletRequest httpRequest, ServletResponse response,
			FilterChain chain, String clientIp)
			throws IOException, ServletException, EopException
	{
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		HttpSession httpSession = httpRequest.getSession(false);

		if (httpSession == null)
			return;

		LoginSession loginSession = (LoginSession) httpSession
				.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		if (loginSession == null)
			return;
		if (loginSession.isExpiredAt(System.currentTimeMillis()))
			return;
		
		dosGauard.successfulAttempt(clientIp);
		if (!loginSession.isUsedOutdatedCredential())
		{
			String loginSessionId = loginSession.getId();
			try
			{
				if (!isVaadinBackgroundRequest(httpRequest))
				{
					log.trace("Update session activity for " + loginSessionId);
					sessionMan.updateSessionActivity(loginSessionId);
				}
				gotoProtectedResource(httpRequest, response, chain, loginSession);
				throw new EopException();
			} catch (IllegalArgumentException e)
			{
				log.debug("Can't update session activity ts for " + loginSessionId
						+ " - expired(?), HTTP session "
						+ httpSession.getId(), e);
			}
		} else
		{
			log.trace("Outdated credential used - redirect to authN");
			forwardtoAuthn(httpRequest, httpResponse, chain);
			throw new EopException();
		}
	}

	private void handleBlockedIP(HttpServletResponse httpResponse, String clientIp)
			throws IOException, EopException
	{
		long blockedTime = dosGauard.getRemainingBlockedTime(clientIp);
		if (blockedTime > 0)
		{
			log.warn("Blocked potential DoS/brute force authN attack from "
					+ clientIp);
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
					"Access is blocked for "
							+ TimeUnit.MILLISECONDS
									.toSeconds(blockedTime)
							+ "s more, due to sending too many invalid session cookies.");
			throw new EopException();
		}
	}

	private void handleSessionFromCookie(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, FilterChain chain, String clientIp)
			throws IOException, ServletException, EopException
	{
		String loginSessionId = CookieHelper.getCookie(httpRequest, sessionCookieName);
		if (loginSessionId == null)
		{
			return;
		}

		try
		{
			LoginSession ls = sessionMan.getSession(loginSessionId);
			bindSessionAndGotoProtectedResource(httpRequest, httpResponse, chain, ls,
					clientIp);
			throw new EopException();

		} catch (IllegalArgumentException e)
		{
			log.trace("Got request with invalid login session id " + loginSessionId
					+ " to " + httpRequest.getRequestURI());
			dosGauard.unsuccessfulAttempt(clientIp);
			clearSessionCookie(httpResponse);
		}
	}

	private void handleRememberMe(HttpServletRequest httpRequest, ServletResponse response,
			FilterChain chain, String clientIp)
			throws IOException, ServletException, EopException
	{
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		Optional<LoginSession> loginSessionFromRememberMe = rememberMeHelper
				.processRememberedWholeAuthn(httpRequest, httpResponse, clientIp,
						realm, dosGauard);
		if (loginSessionFromRememberMe.isEmpty())
		{
			forwardtoAuthn(httpRequest, httpResponse, chain);
			throw new EopException();
		}

		log.info("Whole authn is remembered by entity "
				+ loginSessionFromRememberMe.get().getEntityId() + ", skipping it");
		bindSessionAndGotoProtectedResource(httpRequest, httpResponse, chain,
				loginSessionFromRememberMe.get(), clientIp);
		throw new EopException();
	}

	private void forwardtoAuthn(HttpServletRequest httpRequest, HttpServletResponse response, FilterChain chain)
			throws IOException, EopException, ServletException
	{
		noSessionFilter.doFilter(httpRequest, response);
		chain.doFilter(httpRequest, response);
	}

	private void bindSessionAndGotoProtectedResource(HttpServletRequest httpRequest,
			ServletResponse response, FilterChain chain, LoginSession loginSession,
			String clientIp) throws IOException, ServletException
	{
		dosGauard.successfulAttempt(clientIp);
		sessionBinder.bindHttpSession(httpRequest.getSession(true), loginSession);
		gotoProtectedResource(httpRequest, response, chain, loginSession);
	}

	private void gotoProtectedResource(HttpServletRequest httpRequest, ServletResponse response,
			FilterChain chain, LoginSession session) throws IOException, ServletException
	{
		if (log.isTraceEnabled())
			log.trace("Request to protected address, user is authenticated: "
					+ httpRequest.getRequestURI());
		MDC.put(MDCKeys.USER.key, session.getEntityLabel());
		MDC.put(MDCKeys.ENTITY_ID.key, session.getEntityId());
		try
		{
			UserAuthentication userAuthentication = new UserAuthentication("basic", new DefaultUserIdentity(
					new Subject(),
					new BasicUserPrincipal(String.valueOf(session.getEntityId())),
					new String[]{"USER"})
			);
			((Request) httpRequest).setAuthentication(userAuthentication);
			chain.doFilter(httpRequest, response);
		} finally
		{
			MDC.remove(MDCKeys.USER.key);
			MDC.remove(MDCKeys.ENTITY_ID.key);
		}
	}

	private void gotoNotProtectedResource(HttpServletRequest httpRequest,
			ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		if (log.isTraceEnabled())
			log.trace("Request to not protected address: "
					+ httpRequest.getRequestURI());
		chain.doFilter(httpRequest, response);
	}

	private void clearSessionCookie(HttpServletResponse response)
	{
		response.addCookie(CookieHelper.setupHttpCookie(sessionCookieName, "", 0));
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void init(FilterConfig filterConfig)
	{
	}
	
	public interface NoSessionFilter
	{
		void doFilter(HttpServletRequest httpRequest,
				HttpServletResponse response) throws EopException, IOException;
	}

	public static boolean isVaadinBackgroundRequest(HttpServletRequest request)
	{
		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.isBlank())
			return false;
		return isVaadin23PushRequest(request) || isVaadin23HeartbeatRequest(request);
	}

	private static boolean isVaadin23PushRequest(HttpServletRequest request)
	{
		return request.getMethod().equalsIgnoreCase("get") &&
				ofNullable(request.getParameter("v-r")).orElse("").equalsIgnoreCase("push") &&
				request.getPathInfo().equals("/");
	}

	private static boolean isVaadin23HeartbeatRequest(HttpServletRequest request)
	{
		return request.getMethod().equalsIgnoreCase("post") &&
				"heartbeat".equalsIgnoreCase(request.getParameter("v-r")) &&
				request.getPathInfo().equals("/");
	}

}
