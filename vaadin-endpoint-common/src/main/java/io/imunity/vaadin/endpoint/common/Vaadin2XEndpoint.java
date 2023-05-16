/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServlet;
import eu.unicore.util.configuration.ConfigurationException;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import java.io.StringReader;
import java.util.EventListener;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.setCurrentWebAppAuthenticationFlows;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

public abstract class Vaadin2XEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	protected ApplicationContext applicationContext;
	protected CustomResourceProvider resourceProvider;
	protected String uiServletPath;

	protected ServletContextHandler context = null;
	protected UnityServerConfiguration serverConfig;
	protected MessageSource msg;

	protected InvocationContextSetupFilter contextSetupFilter;
	protected Vaadin82XEndpointProperties genericEndpointProperties;
	protected final RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;
	protected final SandboxAuthnRouter sandboxAuthnRouter;
	protected final Class<? extends VaadinServlet> servletClass;

	public Vaadin2XEndpoint(NetworkServer server,
	                        AdvertisedAddressProvider advertisedAddrProvider,
	                        MessageSource msg,
	                        ApplicationContext applicationContext,
	                        CustomResourceProvider resourceProvider,
	                        String servletPath,
	                        RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
	                        SandboxAuthnRouter sandboxAuthnRouter,
	                        Class<? extends VaadinServlet> servletClass)
	{
		super(server, advertisedAddrProvider);
		this.msg = msg;
		this.applicationContext = applicationContext;
		this.resourceProvider = resourceProvider;
		this.uiServletPath = servletPath;
		this.remoteAuthnResponseProcessingFilter = remoteAuthnResponseProcessingFilter;
		this.serverConfig = applicationContext.getBean(UnityServerConfiguration.class);
		this.sandboxAuthnRouter = sandboxAuthnRouter;
		this.servletClass = servletClass;
	}

	protected abstract ServletContextHandler getServletContextHandlerOverridable(WebAppContext webAppContext);


	@Override
	public void setSerializedConfiguration(String cfg)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(cfg));
			genericEndpointProperties = new Vaadin82XEndpointProperties(properties, serverConfig.getValue(DEFAULT_WEB_CONTENT_PATH));

		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic web"
					+ " endpoint's configuration", e);
		}
	}

	protected String getWebContentsDir()
	{
		if (genericEndpointProperties.isSet(VaadinEndpointProperties.WEB_CONTENT_PATH))
			return genericEndpointProperties.getValue(VaadinEndpointProperties.WEB_CONTENT_PATH);
		if (serverConfig.isSet(DEFAULT_WEB_CONTENT_PATH))
			return serverConfig.getValue(DEFAULT_WEB_CONTENT_PATH);
		return null;
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		Vaadin2XWebAppContext vaadin2XWebAppContext = new Vaadin2XWebAppContext(
				properties, genericEndpointProperties, msg, description, authenticationFlows, null, sandboxAuthnRouter
		);
		context = getServletContextHandlerOverridable(vaadin2XWebAppContext);
		return context;
	}

	protected WebAppContext getWebAppContext(WebAppContext context, String contextPath, Set<String> classPathElements, String webResourceRootUri,
	                               EventListener eventListener) {
		context.setResourceBase(webResourceRootUri);
		context.setContextPath(contextPath);
		context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", JarGetter.getJarsRegex(classPathElements));
		context.setConfigurationDiscovered(true);
		context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		ServletHolder servletHolder = context.addServlet(servletClass, "/*");
		servletHolder.setAsyncSupported(true);
		servletHolder.setInitParameter(InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS, "true");
		context.getServletContext().setExtendedListenerTypes(true);
		if(eventListener != null)
			context.addEventListener(eventListener);

		return context;
	}

	@Override
	public synchronized void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		setAuthenticators(authenticators);
		setCurrentWebAppAuthenticationFlows(authenticators);
	}
}
