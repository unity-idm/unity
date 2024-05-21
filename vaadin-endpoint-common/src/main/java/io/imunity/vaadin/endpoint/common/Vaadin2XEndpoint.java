/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import static org.eclipse.jetty.ee10.webapp.MetaInfConfiguration.CONTAINER_JAR_PATTERN;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

import java.io.StringReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.ee10.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.FragmentConfiguration;
import org.eclipse.jetty.ee10.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.webapp.WebInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebXmlConfiguration;
import org.eclipse.jetty.util.resource.URLResourceFactory;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.startup.ServletContextListeners;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;

public abstract class Vaadin2XEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	public static final String SANDBOX_PATH_ASSOCIATION = "/sandbox-association";
	protected static final Duration UNRESTRICTED_SESSION_TIMEOUT_VALUE = Duration.of(1, ChronoUnit.HOURS);

	protected ApplicationContext applicationContext;
	protected CustomResourceProvider resourceProvider;
	protected String uiServletPath;

	protected ServletContextHandler context = null;
	protected UnityServerConfiguration serverConfig;
	protected MessageSource msg;

	protected InvocationContextSetupFilter contextSetupFilter;
	protected VaadinEndpointProperties genericEndpointProperties;
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
			genericEndpointProperties = new VaadinEndpointProperties(
					properties
			);

		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic web"
					+ " endpoint's configuration", e);
		}
	}

	private String getWebContentsDir()
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

	protected WebAppContext getWebAppContext(WebAppContext context)
	{
		context.setBaseResource(new URLResourceFactory().newResource(getWebContentsDir()));
		context.setContextPath(description.getEndpoint().getContextAddress());
		context.setAttribute(CONTAINER_JAR_PATTERN, JarGetter.getJarsRegex(resourceProvider.getChosenClassPathElement()));
		context.setConfigurationDiscovered(true);
		context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		context.getServerClassMatcher().exclude("org.eclipse.jetty.");
		context.setConfigurations(new Configuration[] {
				new AnnotationConfiguration(),
				new WebAppConfiguration(),
				new WebInfConfiguration(),
				new WebXmlConfiguration(),
				new MetaInfConfiguration(),
				new FragmentConfiguration(),
				new JettyWebXmlConfiguration(),
		});

		ServletHolder servletHolder = context.addServlet(servletClass, "/*");
		servletHolder.setAsyncSupported(true);
		servletHolder.setInitParameter(InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS, "true");
		context.addEventListener(new ServletContextListeners());

		return context;
	}

	@Override
	public synchronized void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		setAuthenticators(authenticators);
		((Vaadin2XWebAppContext) context).setAuthenticationFlows(authenticationFlows);
	}
}
