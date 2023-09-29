/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.utils.CookieEE10Helper;
import pl.edu.icm.unity.stdext.identity.X500Identity;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

/**
 * Servlet filter responsible merely for setting up {@link InvocationContext}. All servlets relaying on 
 * {@link InvocationContext} should be wrapped by this filter. This filter must be installed after the authentication
 * filter, if the latter is used.
 * @author K. Benedyczak
 */
public class InvocationContextSetupFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			InvocationContextSetupFilter.class);
	public static final String LANGUAGE_COOKIE = "language";
	
	private UnityServerConfiguration config;
	private AuthenticationRealm realm;
	private String baseAddress;
	private List<AuthenticationFlow> authenticationFlows;
	
	/**
	 * @param baseAddress public address of the server with scheme and port, with empty path.
	 */
	public InvocationContextSetupFilter(UnityServerConfiguration config, 
			AuthenticationRealm realm, String baseAddress, List<AuthenticationFlow> authenticationFlows)
	{
		this.realm = realm;
		this.config = config;
		this.baseAddress = baseAddress;
		this.authenticationFlows = authenticationFlows;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		
		InvocationContext ctx = createInvocationContext(httpRequest);
		setLoginSession(httpRequest, ctx);
		setLocale(httpRequest, ctx);
		try
		{
			chain.doFilter(httpRequest, response);
		} finally
		{
			InvocationContext.setCurrent(null);
		}
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}
	
	private InvocationContext createInvocationContext(HttpServletRequest request)
	{
		X509Certificate[] clientCert = (X509Certificate[]) request.getAttribute(
				"javax.servlet.request.X509Certificate");
		IdentityTaV tlsId = (clientCert == null) ? null : new IdentityTaV(X500Identity.ID, 
				clientCert[0].getSubjectX500Principal().getName());
		InvocationContext context = new InvocationContext(tlsId, realm, authenticationFlows);
		InvocationContext.setCurrent(context);
		if (baseAddress != null)
			context.setCurrentURLUsed(baseAddress);
		log.trace("A new invocation context was set");
		return context;
	}
	
	private void setLoginSession(HttpServletRequest request, InvocationContext ctx)
	{
		HttpSession session = request.getSession(false);
		if (session != null)
		{
			LoginSession ls = (LoginSession) session.getAttribute(
					LoginToHttpSessionBinder.USER_SESSION_KEY);
			if (ls != null)
			{
				ctx.setLoginSession(ls);
				log.trace("Login session was set for the invocation context");
			}
		}
	}
	
	
	/**
	 * Sets locale in invocation context. If there is cookie with selected and still supported
	 * locale then it is used. Otherwise a default locale is set.
	 */
	private void setLocale(HttpServletRequest request, InvocationContext context)
	{
		String value = CookieEE10Helper.getCookie(request, LANGUAGE_COOKIE);
		if (value != null)
		{
			Locale locale = UnityServerConfiguration.safeLocaleDecode(value);
			if (config.isLocaleSupported(locale))
			{
				context.setLocale(locale);
				log.trace("Requested locale was set for the invocation context");
				return;
			}
		}
		context.setLocale(config.getDefaultLocale());
		log.trace("Default locale was set for the invocation context");
	}
}
