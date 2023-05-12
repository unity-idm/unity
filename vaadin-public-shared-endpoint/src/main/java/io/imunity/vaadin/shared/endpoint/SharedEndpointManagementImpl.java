/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.shared.endpoint;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.startup.ServletContextListeners;
import io.imunity.vaadin.endpoint.common.JarGetter;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
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
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import javax.servlet.DispatcherType;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static io.imunity.vaadin.elements.VaadinInitParameters.SESSION_TIMEOUT_PARAM;
import static java.util.Collections.emptyList;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

@Primary
@Component
public class SharedEndpointManagementImpl implements SharedEndpointManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, SharedEndpointManagementImpl.class);
	private static final String ENDPOINT_ID = "sys:sharedV23";
	private static final Duration SESSION_TIMEOUT_VALUE = Duration.of(1, ChronoUnit.HOURS);
	private final ServletContextHandler sharedHandler;
	private final URL advertisedAddress;
	private final Set<String> usedPaths;
	
	@Autowired
	public SharedEndpointManagementImpl(NetworkServer httpServer,
	                                    UnityServerConfiguration config,
	                                    AdvertisedAddressProvider advertisedAddrProvider,
	                                    SharedResourceProvider sharedResourceProvider,
	                                    MessageSource msg,
	                                    RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter) throws EngineException
	{
		Properties properties = config.getProperties();
		Vaadin82XEndpointProperties vaadinEndpointProperties = new Vaadin82XEndpointProperties(properties, config.getValue(DEFAULT_WEB_CONTENT_PATH));
		WebAppContext context = new Vaadin2XWebAppContext(properties, vaadinEndpointProperties, msg, null);
		context.setResourceBase(getWebContentsDir(config));
		context.setContextPath(CONTEXT_PATH);
		context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", JarGetter.getJarsRegex(sharedResourceProvider.getChosenClassPathElement()));
		context.setConfigurationDiscovered(true);
		context.getServletContext().setExtendedListenerTypes(true);
		context.addEventListener(new ServletContextListeners());

		context.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*",
				EnumSet.of(DispatcherType.REQUEST));
		context.addFilter(new FilterHolder(new InvocationContextSetupFilter(config, null, null, emptyList())), "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		ServletHolder servletHolder = context.addServlet(SimpleVaadin2XServlet.class, "/*");
		servletHolder.setAsyncSupported(true);
		servletHolder.setInitParameter(InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS, "true");
		servletHolder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(SESSION_TIMEOUT_VALUE.getSeconds()));

		httpServer.deployHandler(context, ENDPOINT_ID);

		sharedHandler = context;
		usedPaths = new HashSet<>();
		this.advertisedAddress = advertisedAddrProvider.get();
	}


	public String getServerAddress()
	{
		return advertisedAddress.toExternalForm();
	}
	
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


	public void deployInternalEndpointFilter(String contextPath, FilterHolder filter)
	{
		sharedHandler.addFilter(filter, contextPath + "/*", EnumSet.of(DispatcherType.REQUEST));
		log.debug("Deployed internal servlet filter" + filter.getClassName() + " at: " +
				CONTEXT_PATH + contextPath);
	}
	
	public String getBaseContextPath()
	{
		return CONTEXT_PATH;
	}
	
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
