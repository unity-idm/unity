/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.net.URL;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.DispatcherType;

import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;


/**
 * Implementation of the {@link SharedEndpointManagement}
 * @author K. Benedyczak
 */
@Component
public class SharedEndpointManagementImpl implements SharedEndpointManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, SharedEndpointManagementImpl.class);
	public static final String CONTEXT_PATH = "/unitygw";
	public static final String VAADIN_RESOURCE_PATH = "/VAADIN/*";
	private ServletContextHandler sharedHandler;
	private URL advertisedAddress;
	private Set<String> usedPaths;
	
	@Autowired
	public SharedEndpointManagementImpl(JettyServer httpServer, UnityServerConfiguration config) throws EngineException
	{
		sharedHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		sharedHandler.setContextPath(CONTEXT_PATH);
		sharedHandler.getServletContext().getSessionCookieConfig().setHttpOnly(true);
		String resourceBase = getWebContentsDir(config);
		if (resourceBase != null)
			sharedHandler.setResourceBase(resourceBase);
		httpServer.deployHandler(sharedHandler);
		usedPaths = new HashSet<>();
		this.advertisedAddress = httpServer.getAdvertisedAddress();
	}

	@Override
	public String getServerAddress()
	{
		return advertisedAddress.toExternalForm();
	}
	
	@Override
	public void deployInternalEndpointServlet(String contextPath, ServletHolder servlet, boolean mapVaadinResource)
			throws EngineException
	{
		if (usedPaths.contains(contextPath))
			throw new WrongArgumentException("The context path " + contextPath + " is already assigned.");
		sharedHandler.addServlet(servlet, contextPath + "/*");
		if (mapVaadinResource && !usedPaths.contains(VAADIN_RESOURCE_PATH))
		{
			usedPaths.add(VAADIN_RESOURCE_PATH);
			sharedHandler.addServlet(servlet, VAADIN_RESOURCE_PATH);
		}
		log.debug("Deployed internal servlet " + servlet.getClassName() + " at: " +
				CONTEXT_PATH + contextPath);
	}

	@Override
	public void deployInternalEndpointFilter(String contextPath, FilterHolder filter)
			throws EngineException
	{
		sharedHandler.addFilter(filter, contextPath + "/*", EnumSet.of(DispatcherType.REQUEST));
		log.debug("Deployed internal servlet filter" + filter.getClassName() + " at: " +
				CONTEXT_PATH + contextPath);
	}
	
	@Override
	public String getBaseContextPath()
	{
		return CONTEXT_PATH;
	}
	
	@Override
	public String getServletUrl(String servletPath)
	{
		return advertisedAddress.toExternalForm() +
				getBaseContextPath() + 
				servletPath;
	}
	

	protected String getWebContentsDir(UnityServerConfiguration config)
	{
		if (config.isSet(UnityServerConfiguration.UNITYGW_WEB_CONTENT_PATH))
			return config.getValue(UnityServerConfiguration.UNITYGW_WEB_CONTENT_PATH);
		if (config.isSet(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))
			return config.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		return null;
	}
}
