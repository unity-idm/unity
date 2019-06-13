/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import io.imunity.authproxy.oauth.OAuthClient;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Performs authentication via configured Unity AS. All GET requests trigger new authentication flow (code grant),
 * besides a GET request with oauth return redirect params - this one triggers response processing: token exchange,
 * fetching of user's data and finally setup of local user's session.
 */
@Component
class OAuthAuthenticationServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, OAuthAuthenticationServlet.class);
	private static final String STATE_ATTR = "OAuthAuthenticationServlet.OAUTH-STATE";
	
	private final TokenVerificator verificator;
	private final OAuthClient oauthClient;
	
	@Autowired
	OAuthAuthenticationServlet(TokenVerificator verificator, OAuthClient oauthClient)
	{
		this.verificator = verificator;
		this.oauthClient = oauthClient;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try
		{
			doGetUnsafe(req, resp);
		} catch (Exception e)
		{
			log.error("Received authentication error", e);
			resp.sendError(403, "Authentication problem");
		}
	}

	private void doGetUnsafe(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		HttpSession session = req.getSession(false);
		if (session == null)
		{
			log.info("Starting new authn - no session");
			startNewAuthn(req, resp);
			return;
		}
		String recordedOauthState = (String) session.getAttribute(STATE_ATTR);
		String codeParam = req.getParameter("code");
		String stateParam = req.getParameter("state");
		log.info("Got code: {} state: {} State in session is: {}", codeParam, stateParam, recordedOauthState);
		if (recordedOauthState != null && codeParam != null && Objects.equal(stateParam, recordedOauthState))
		{
			log.info("Processing OAuth response");			
			processResponse(req, resp, codeParam);
		} else
		{
			log.info("Starting new authentication - no matching OAuth response found in request");
			startNewAuthn(req, resp);
		}
	}

	
	private void processResponse(HttpServletRequest req, HttpServletResponse resp, String code) throws IOException
	{
		UserAttributes userAttributes = oauthClient.getAccessTokenAndProfile(code);
		log.info("Success login: {}", userAttributes);
		String token = UUID.randomUUID().toString();
		verificator.registerUser(token, userAttributes);
		Cookie cookie = new Cookie(AuthenticationCheckingFilter.COOKIE, token);
		cookie.setMaxAge(-1);
		resp.addCookie(cookie);
		resp.sendRedirect("/");
	}

	private void startNewAuthn(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		HttpSession session = req.getSession();
		String state = UUID.randomUUID().toString();
		session.setAttribute(STATE_ATTR, state);
		URI requestURI = oauthClient.createRequest(state);
		resp.sendRedirect(requestURI.toASCIIString());
	}

}
