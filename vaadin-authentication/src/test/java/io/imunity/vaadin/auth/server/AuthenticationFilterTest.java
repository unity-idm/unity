/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.server;

import io.imunity.vaadin.endpoint.common.EopException;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.mockito.Mockito.*;

class AuthenticationFilterTest
{

	@Test
	public void shouldForwardToAuthnWhenRealmsAreNotEqual() throws IOException, ServletException, EopException
	{

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		LoginSession loginSession = Mockito.mock(LoginSession.class);
		AuthenticationFilter.NoSessionFilter noSessionFilter = Mockito.mock(AuthenticationFilter.NoSessionFilter.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		AuthenticationRealm adminRealm = new AuthenticationRealm("admin", "description", 1000, 1000, RememberMePolicy.allowForWholeAuthn, 1000, 1000);
		AuthenticationFilter filter = new AuthenticationFilter(adminRealm, null, null, null, noSessionFilter);

		HTTPRequestContext.setCurrent(new HTTPRequestContext("client", "agent"));
		when(request.getServletPath()).thenReturn("/secured");
		when(request.getSession()).thenReturn(session);
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY)).thenReturn(loginSession);
		when(loginSession.getRealm()).thenReturn("home");

		filter.doFilter(request, response, chain);

		verify(noSessionFilter).doFilter(request, response);
	}

	@Test
	public void shouldGoToProtectedResourceWhenRealmsAreEqual() throws IOException, ServletException, EopException
	{
		Request request = Mockito.mock(Request.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		LoginSession loginSession = Mockito.mock(LoginSession.class);
		FilterChain chain = Mockito.mock(FilterChain.class);
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		AuthenticationFilter.NoSessionFilter noSessionFilter = Mockito.mock(AuthenticationFilter.NoSessionFilter.class);

		AuthenticationRealm adminRealm = new AuthenticationRealm("admin", "description", 1000, 1000, RememberMePolicy.allowForWholeAuthn, 1000, 1000);
		AuthenticationFilter filter = new AuthenticationFilter(adminRealm, sessionManagement, null, null);

		HTTPRequestContext.setCurrent(new HTTPRequestContext("client", "agent"));
		when(request.getServletPath()).thenReturn("/secured");
		when(request.getSession()).thenReturn(session);
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY)).thenReturn(loginSession);
		when(loginSession.getRealm()).thenReturn("admin");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verify(noSessionFilter, times(0)).doFilter(request, response);
	}

}