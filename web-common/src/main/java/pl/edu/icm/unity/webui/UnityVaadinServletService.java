/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;

/**
 * Extension of the {@link VaadinServletService}. It sets up the Unity's thread locals for each request 
 * from the session.
 * <p>
 * This process should be done here, not on the servlet level to handle also requests coming via async (websocket)
 * channel.
 * 
 * @author K. Benedyczak
 */
public class UnityVaadinServletService extends VaadinServletService
{
	public static final String LANGUAGE_COOKIE = "language";
	private transient UnityServerConfiguration config;
	
	public UnityVaadinServletService(UnityServerConfiguration config, VaadinServlet servlet,
			DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		super(servlet, deploymentConfiguration);
		this.config = config;
	}

	@Override
	public void requestStart(VaadinRequest request, VaadinResponse response)
	{
		super.requestStart(request, response);
		InvocationContext ctx = setEmptyInvocationContext();
		setAuthenticationContext((VaadinServletRequest) request, ctx);
		setLocale((VaadinServletRequest) request, ctx);
	}

	@Override
	public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session)
	{
		InvocationContext.setCurrent(null);
		super.requestEnd(request, response, session);
	}
		    
	private InvocationContext setEmptyInvocationContext()
	{
		InvocationContext context = new InvocationContext();
		InvocationContext.setCurrent(context);
		return context;
	}
	
	private void setAuthenticationContext(VaadinServletRequest request, InvocationContext ctx)
	{
		HttpSession session = request.getSession(false);
		if (session != null)
		{
			AuthenticatedEntity ae = (AuthenticatedEntity) session.getAttribute(
					WebSession.USER_SESSION_KEY);
			if (ae != null)
				ctx.setAuthenticatedEntity(ae);
		}
	}
	
	/**
	 * Sets locale in invocation context. If there is cookie with selected and still supported
	 * locale then it is used. Otherwise a default locale is set.
	 * @param request
	 */
	private void setLocale(VaadinServletRequest request, InvocationContext context)
	{
		Cookie[] cookies = request.getCookies();
		for (Cookie cookie: cookies)
		{
			if (LANGUAGE_COOKIE.equals(cookie.getName()))
			{
				String value = cookie.getValue();
				Locale locale = UnityServerConfiguration.safeLocaleDecode(value);
				if (config.isLocaleSupported(locale))
					context.setLocale(locale);
				else
					context.setLocale(config.getDefaultLocale());
				return;
			}
		}
		context.setLocale(config.getDefaultLocale());
	}
}
