/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.confirmations;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.startup.ServletContextListeners;
import io.imunity.vaadin23.endpoint.common.JarGetter;
import io.imunity.vaadin23.endpoint.common.Vaadin23WebAppContext;
import io.imunity.vaadin23.endpoint.common.Vaadin823EndpointProperties;
import io.imunity.vaadin23.shared.endpoint.SharedResourceProvider;
import io.imunity.vaadin23.shared.endpoint.SimpleVaadin23Servlet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

import javax.servlet.DispatcherType;
import java.net.URL;
import java.util.*;

import static java.util.Collections.emptyList;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

@Primary
@Component
public class SharedV23EndpointManagementImpl implements SharedEndpointManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, SharedV23EndpointManagementImpl.class);
	private final ServletContextHandler sharedHandler;
	private final URL advertisedAddress;
	private final Set<String> usedPaths;
	
	@Autowired
	public SharedV23EndpointManagementImpl(NetworkServer httpServer,
	                                       UnityServerConfiguration config,
	                                       AdvertisedAddressProvider advertisedAddrProvider,
	                                       SharedResourceProvider sharedResourceProvider,
	                                       MessageSource msg) throws EngineException
	{
		Properties properties = config.getProperties();
		Vaadin823EndpointProperties vaadinEndpointProperties = new Vaadin823EndpointProperties(properties, config.getValue(DEFAULT_WEB_CONTENT_PATH));
		WebAppContext context = new Vaadin23WebAppContext(properties, vaadinEndpointProperties, msg, null);
		context.setResourceBase(getWebContentsDir(config));
		context.setContextPath(CONTEXT_PATH + "2");
		context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", JarGetter.getJarsRegex(sharedResourceProvider.getChosenClassPathElement()));
		context.setConfigurationDiscovered(true);
		context.getServletContext().setExtendedListenerTypes(true);
		context.addEventListener(new ServletContextListeners());

		context.addFilter(new FilterHolder(new InvocationContextSetupFilter(config, null, null, emptyList())), "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		ServletHolder servletHolder = context.addServlet(SimpleVaadin23Servlet.class, "/*");
		servletHolder.setAsyncSupported(true);
		servletHolder.setInitParameter(InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS, "true");

		httpServer.deployHandler(context, "sys:shared2");

		sharedHandler = context;
		usedPaths = new HashSet<>();
		this.advertisedAddress = advertisedAddrProvider.get();
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
		usedPaths.add(contextPath);
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
