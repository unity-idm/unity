/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationFilterTest
{
	@Test
	public void shouldForwardToAuthnWhenRealmsAreNotEqual() throws IOException, ServletException
	{

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		LoginSession loginSession = Mockito.mock(LoginSession.class);
		RequestDispatcher requestDispatcher = Mockito.mock(RequestDispatcher.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		AuthenticationRealm adminRealm = new AuthenticationRealm("admin", "description", 1000, 1000, RememberMePolicy.allowForWholeAuthn, 1000, 1000);
		AuthenticationFilter filter = new AuthenticationFilter(List.of("secured"), "auth", adminRealm, null, null, null);

		HTTPRequestContext.setCurrent(new HTTPRequestContext("client", "agent"));
		when(request.getServletPath()).thenReturn("/secured");
		when(request.getSession()).thenReturn(session);
		when(request.getSession(false)).thenReturn(session);
		when(request.getRequestDispatcher("auth")).thenReturn(requestDispatcher);
		when(session.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY)).thenReturn(loginSession);
		when(loginSession.getRealm()).thenReturn("home");

		filter.doFilter(request, response, chain);

		verify(requestDispatcher).forward(request, response);
		verify(chain, times(0)).doFilter(request, response);
	}

	@Test
	public void shouldGoToProtectedResourceWhenRealmsAreEqual() throws IOException, ServletException
	{
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		LoginSession loginSession = Mockito.mock(LoginSession.class);
		FilterChain chain = Mockito.mock(FilterChain.class);
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);

		AuthenticationRealm adminRealm = new AuthenticationRealm("admin", "description", 1000, 1000, RememberMePolicy.allowForWholeAuthn, 1000, 1000);
		AuthenticationFilter filter = new AuthenticationFilter(List.of("secured"), "auth", adminRealm, sessionManagement, null, null);

		HTTPRequestContext.setCurrent(new HTTPRequestContext("client", "agent"));
		when(request.getServletPath()).thenReturn("/secured");
		when(request.getSession()).thenReturn(session);
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY)).thenReturn(loginSession);
		when(loginSession.getRealm()).thenReturn("admin");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
	}

}
