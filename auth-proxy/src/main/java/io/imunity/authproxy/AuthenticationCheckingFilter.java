/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;

@Component
class AuthenticationCheckingFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuthenticationCheckingFilter.class);
	
	static final String USER_INFO_ATTR = AuthenticationCheckingFilter.class.getCanonicalName() + ".userInfo";
	static final String COOKIE = "UAuthnCookie";
	static final String INTERNAL_AUTHN_PATH = "/internal-authn";

	private final TokenVerificator tokenVerificator;

	AuthenticationCheckingFilter(TokenVerificator tokenVerificator)
	{
		this.tokenVerificator = tokenVerificator;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;

		String tokenFromCookie = getTokenFromCookie(httpRequest);
		if (tokenFromCookie != null)
		{
			log.debug("Request has cookie");
			UserAttributes userAttributes = tokenVerificator.get(tokenFromCookie);
			if (userAttributes != null)
			{
				log.debug("Request is authenticated, letting in");
				injectUserInformation(httpRequest, userAttributes);
				chain.doFilter(httpRequest, response);
				return;
			}
		}
		log.debug("Request is not authenticated, forwarding to authn");
		httpRequest.getRequestDispatcher(INTERNAL_AUTHN_PATH).forward(httpRequest, response);
	}

	private void injectUserInformation(HttpServletRequest httpRequest, UserAttributes userAttributes)
	{
		httpRequest.setAttribute(USER_INFO_ATTR, userAttributes);
	}
	
	private String getTokenFromCookie(HttpServletRequest httpRequest)
	{
		Cookie[] cookies = httpRequest.getCookies();
		if (cookies != null)
		{
			for (Cookie cookie: cookies)
			{
				if (COOKIE.equals(cookie.getName()))
					return cookie.getValue();
			}
		}
		return null;
	}
	
	@Override
	public void destroy()
	{
	}
}
