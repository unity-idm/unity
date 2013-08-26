/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.bus.EventsBus;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;


/**
 * Customization of the ordinary {@link VaadinServlet} using {@link VaadinUIProvider}
 * @author K. Benedyczak
 */
@SuppressWarnings("serial")
public class UnityVaadinServlet extends VaadinServlet
{
	public static final String LANGUAGE_COOKIE = "language";
	private transient ApplicationContext applicationContext;
	private transient UnityServerConfiguration config;
	private transient String uiBeanName;
	private transient EndpointDescription description;
	private transient List<Map<String, BindingAuthn>> authenticators;
	private transient CancelHandler cancelHandler;
	
	public UnityVaadinServlet(ApplicationContext applicationContext, String uiBeanName,
			EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		super();
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.description = description;
		this.authenticators = authenticators;
		this.config = applicationContext.getBean(UnityServerConfiguration.class);
	}
	
	public void setCancelHandler(CancelHandler cancelHandler)
	{
		this.cancelHandler = cancelHandler;
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException 
	{
		InvocationContext ctx = setEmptyInvocationContext();
		setAuthenticationContext(request, ctx);
		setLocale(request, ctx);
		getService().addSessionInitListener(new VaadinSessionInit());
		try
		{
			super.service(request, response);
		} finally 
		{
			InvocationContext.setCurrent(null);
		}
	}
	
	private InvocationContext setEmptyInvocationContext()
	{
		InvocationContext context = new InvocationContext();
		InvocationContext.setCurrent(context);
		return context;
	}
	
	private void setAuthenticationContext(HttpServletRequest request, InvocationContext ctx)
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
	private void setLocale(HttpServletRequest request, InvocationContext context)
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
	
	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
	{
		final VaadinServletService service = super.createServletService(deploymentConfiguration);

		service.addSessionInitListener(new SessionInitListener()
		{
			@Override
			public void sessionInit(SessionInitEvent event) throws ServiceException
			{
				VaadinUIProvider uiProv = new VaadinUIProvider(applicationContext, uiBeanName,
						description, authenticators);
				uiProv.setCancelHandler(cancelHandler);
				event.getSession().addUIProvider(uiProv);
				DeploymentConfiguration depCfg = event.getService().getDeploymentConfiguration();
				Properties properties = depCfg.getInitParameters();
				String timeout = properties.getProperty(VaadinEndpoint.SESSION_TIMEOUT_PARAM);
				if (timeout != null)
					event.getSession().getSession().setMaxInactiveInterval(Integer.parseInt(timeout));
			}
		});

		return service;
	}
	
	private static class VaadinSessionInit implements SessionInitListener
	{
		@Override
		public void sessionInit(SessionInitEvent event) throws ServiceException
		{
			if (WebSession.getCurrent() == null)
			{
				WebSession webSession = new WebSession(new EventsBus());
				WebSession.setCurrent(webSession);
			}			
		}
	}
}
