/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.server.authn.AuthenticationContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

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
	private transient ApplicationContext applicationContext;
	private transient String uiBeanName;
	private transient EndpointDescription description;
	private transient List<Map<String, BindingAuthn>> authenticators;
	
	public UnityVaadinServlet(ApplicationContext applicationContext, String uiBeanName,
			EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		super();
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.description = description;
		this.authenticators = authenticators;
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException 
	{
		setAuthenticationcontext(request);
		try
		{
			super.service(request, response);
		} finally 
		{
			AuthenticationContext.setCurrent(null);
		}
	}

	private void setAuthenticationcontext(HttpServletRequest request)
	{
		HttpSession session = request.getSession(false);
		if (session != null)
		{
			AuthenticationContext authnContext = (AuthenticationContext) session.getAttribute(
					WebSession.USER_SESSION_KEY);
			if (authnContext != null)
				AuthenticationContext.setCurrent(authnContext);
		}
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
				event.getSession().addUIProvider(uiProv);
			}
		});

		return service;
	}
}
