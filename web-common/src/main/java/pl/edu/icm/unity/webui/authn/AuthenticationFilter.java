/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ApplicationConstants;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.CookieHelper;
import pl.edu.icm.unity.webui.idpcommon.EopException;

/**
 * Servlet filter forwarding unauthenticated requests to the protected authentication servlet.
 * @author K. Benedyczak
 */
public class AuthenticationFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationFilter.class);

	private List<String> protectedServletPaths;
	private String authnServletPath;
	private final String sessionCookie;
	private UnsuccessfulAuthenticationCounter dosGauard;
	private SessionManagement sessionMan;
	private LoginToHttpSessionBinder sessionBinder;
	private RememberMeProcessor rememberMeHelper;
	private AuthenticationRealm realm;
	
	public AuthenticationFilter(List<String> protectedServletPaths, String authnServletPath, 
			AuthenticationRealm realm,
			SessionManagement sessionMan, LoginToHttpSessionBinder sessionBinder, RememberMeProcessor rememberMeHelper)
	{
		this.protectedServletPaths = new ArrayList<>(protectedServletPaths);
		this.authnServletPath = authnServletPath;
		dosGauard = new UnsuccessfulAuthenticationCounter(realm.getBlockAfterUnsuccessfulLogins(), 
				realm.getBlockFor()*1000);
		sessionCookie = StandardWebAuthenticationProcessor.getSessionCookieName(realm.getName());
		this.sessionMan = sessionMan;
		this.sessionBinder = sessionBinder;
		this.rememberMeHelper = rememberMeHelper;
		this.realm = realm;
	}
	
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		String clientIp = request.getRemoteAddr();
	
		try 
		{
			handleNotProtectedResource(httpRequest, httpResponse, chain);
			handleBoundSession(httpRequest, httpResponse, chain, clientIp);
			handleBlockedIP(httpResponse, clientIp);
			handleSessionFromCookie(httpRequest, httpResponse, chain, clientIp);
			handleRememberMe(httpRequest, httpResponse, chain, clientIp);
		
		} catch (EopException e)
		{
			return;
	
		} 
		
		//it should not happen, for safety only  
		forwardtoAuthn(httpRequest, httpResponse);
	}

	private void handleNotProtectedResource(HttpServletRequest httpRequest,
			ServletResponse response, FilterChain chain)
			throws IOException, ServletException, EopException
	{
		String servletPath = httpRequest.getServletPath();
		if (!HiddenResourcesFilter.hasPathPrefix(servletPath, protectedServletPaths))
		{
			gotoNotProtectedResource(httpRequest, response, chain);
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
				if (!HiddenResourcesFilter.hasPathPrefix(httpRequest.getPathInfo(),
						ApplicationConstants.HEARTBEAT_PATH + '/'))
				{
					log.trace("Update session activity for " + loginSessionId);
					sessionMan.updateSessionActivity(loginSessionId);
				} else
				{
					
				}
				gotoProtectedResource(httpRequest, response, chain);
				throw new EopException();
			} catch (IllegalArgumentException e)
			{
				log.debug("Can't update session activity ts for " + loginSessionId
						+ " - expired(?), HTTP session "
						+ httpSession.getId(), e);
				return;
			}
		} else
		{
			log.trace("Outdated credential used - redirect to authN");
			forwardtoAuthn(httpRequest, httpResponse);
			throw new EopException();
		}
	}

	private void handleBlockedIP(HttpServletResponse httpResponse, String clientIp)
			throws IOException, EopException
	{
		long blockedTime = dosGauard.getRemainingBlockedTime(clientIp);
		if (blockedTime > 0)
		{
			log.debug("Blocked potential DoS/brute force authN attack from "
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
		String loginSessionId = CookieHelper.getCookie(httpRequest, sessionCookie);
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
			return;
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
		if (!loginSessionFromRememberMe.isPresent())
		{
			forwardtoAuthn(httpRequest, httpResponse);
			throw new EopException();
		}

		log.debug("Whole authn is remembered by entity "
				+ loginSessionFromRememberMe.get().getEntityId() + ", skipping it");
		bindSessionAndGotoProtectedResource(httpRequest, httpResponse, chain,
				loginSessionFromRememberMe.get(), clientIp);
		throw new EopException();
	}

	private void forwardtoAuthn(HttpServletRequest httpRequest, HttpServletResponse response)
			throws IOException, ServletException
	{
		String forwardURI = authnServletPath;
		if (httpRequest.getPathInfo() != null)
		{
			forwardURI += httpRequest.getPathInfo();
		}
		if (log.isTraceEnabled())
		{
			log.trace("Request to protected address, forward: "
					+ httpRequest.getRequestURI() + " -> "
					+ httpRequest.getContextPath() + forwardURI);
		}
		RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(forwardURI);
		dispatcher.forward(httpRequest, response);
	}

	private void bindSessionAndGotoProtectedResource(HttpServletRequest httpRequest,
			ServletResponse response, FilterChain chain, LoginSession loginSession,
			String clientIp) throws IOException, ServletException
	{
		dosGauard.successfulAttempt(clientIp);
		sessionBinder.bindHttpSession(httpRequest.getSession(true), loginSession);
		gotoProtectedResource(httpRequest, response, chain);
	}

	private void gotoProtectedResource(HttpServletRequest httpRequest, ServletResponse response,
			FilterChain chain) throws IOException, ServletException
	{
		if (log.isTraceEnabled())
			log.trace("Request to protected address, user is authenticated: "
					+ httpRequest.getRequestURI());
		chain.doFilter(httpRequest, response);
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
		response.addCookie(CookieHelper.setupHttpCookie(sessionCookie, "", 0));
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	public void addProtectedPath(String path)
	{
		protectedServletPaths.add(path);
	}

}
