/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;


/**
 * Extends the regular {@link VaadinServletService} by replacing its bootstrap handler with
 * our own {@link UnityBootstrapHandler}.
 * 
 * @author K. Benedyczak
 */
public class UnityVaadinServletService extends VaadinServletService
{
	private UnityBootstrapHandler bootstrapHandler;

	public UnityVaadinServletService(VaadinServlet servlet,
			DeploymentConfiguration deploymentConfiguration, UnityBootstrapHandler bootstrapHandler) 
					throws ServiceException
	{
		super(servlet, deploymentConfiguration);
		this.bootstrapHandler = bootstrapHandler;
	}

	@Override
	protected List<RequestHandler> createRequestHandlers() throws ServiceException 
	{
		List<RequestHandler> handlers = super.createRequestHandlers();
		handlers.set(0, bootstrapHandler);
		return handlers;
	}
}
