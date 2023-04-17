/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.secured_shared_endpoint;

import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.server.Constants;
import io.imunity.vaadin.endpoint.common.CustomResourceProvider;
import io.imunity.vaadin.endpoint.common.Vaadin2XEndpoint;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.UnityBootstrapHandler;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;
import pl.edu.icm.unity.webui.sandbox.AccountAssociationSandboxUI;

import static io.imunity.vaadin.elements.VaadinInitParameters.SESSION_TIMEOUT_PARAM;
import static pl.edu.icm.unity.webui.VaadinEndpoint.*;

class Vaadin2XEndpointWithSandbox extends Vaadin2XEndpoint
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, Vaadin2XEndpointWithSandbox.class);

	Vaadin2XEndpointWithSandbox(NetworkServer server, AdvertisedAddressProvider advertisedAddrProvider,
	                            MessageSource msg, ApplicationContext applicationContext,
	                            CustomResourceProvider resourceProvider, String servletPath,
	                            RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
	                            Class<? extends VaadinServlet> servletClass)
	{
		super(server, advertisedAddrProvider, msg, applicationContext, resourceProvider, servletPath, remoteAuthnResponseProcessingFilter, servletClass);
	}

	@Override
	public synchronized ServletContextHandler getServletContextHandler()
	{
		SandboxAuthnRouter sandboxRouter = new SandboxAuthnRouterImpl();
		Vaadin2XWebAppContextWithSandbox webAppContext = new Vaadin2XWebAppContextWithSandbox(
				properties, genericEndpointProperties, msg, description, authenticationFlows, sandboxRouter
		);
		context = getServletContextHandlerOverridable(webAppContext);
		addSandboxUI(SANDBOX_PATH_ASSOCIATION, AccountAssociationSandboxUI.class.getSimpleName(), sandboxRouter);
		return context;
	}

	private void addSandboxUI(String path, String uiBeanName, SandboxAuthnRouter sandboxRouter)
	{
		UnityBootstrapHandler bootstrapHanlder = getBootstrapHandlerGeneric(path, getHeartbeatInterval(description.getRealm().getMaxInactivity()), genericEndpointProperties.getEffectiveMainTheme());
		UnityVaadinServlet sandboxServlet = new UnityVaadinServlet(applicationContext,
				uiBeanName, description, authenticationFlows, null, properties, bootstrapHanlder);
		sandboxServlet.setSandboxRouter(sandboxRouter);
		ServletHolder sandboxServletHolder = createVaadin8ServletHolder(sandboxServlet);
		sandboxServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(sandboxServletHolder, path + path + "/*");
		context.addServlet(sandboxServletHolder, path + "/*");
	}

	protected int getHeartbeatInterval(int sessionTimeout)
	{
		if (sessionTimeout >= 3*DEFAULT_HEARTBEAT)
			return DEFAULT_HEARTBEAT;
		int ret = sessionTimeout/3;
		return Math.max(ret, 2);
	}

	protected ServletHolder createVaadin8ServletHolder(com.vaadin.server.VaadinServlet servlet)
	{
		ServletHolder holder = new ServletHolder(servlet);
		holder.setInitParameter("closeIdleSessions", "true");

		holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(LONG_SESSION));

		int heartBeat = LONG_HEARTBEAT;
		log.debug("Servlet " + servlet.toString() + " - heartBeat=" +heartBeat);

		boolean productionMode = genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.PRODUCTION_MODE);
		holder.setInitParameter("heartbeatInterval", String.valueOf(heartBeat));
		holder.setInitParameter("sendUrlsAsParameters", "false"); //theoreticly needed for push state navi, but adding this causes NPEs
		holder.setInitParameter(PRODUCTION_MODE_PARAM, String.valueOf(productionMode));
		holder.setInitParameter("org.atmosphere.cpr.broadcasterCacheClass",
				"org.atmosphere.cache.UUIDBroadcasterCache");
		holder.setInitParameter(Constants.PARAMETER_WIDGETSET,
				"pl.edu.icm.unity.webui.customWidgetset");
		return holder;
	}

	private UnityBootstrapHandler getBootstrapHandlerGeneric(String uiPath, int heartBeat, String theme)
	{
		String template = genericEndpointProperties.getValue(VaadinEndpointProperties.TEMPLATE);
		boolean productionMode = genericEndpointProperties.getBooleanValue(
				VaadinEndpointProperties.PRODUCTION_MODE);
		return new UnityBootstrapHandler(getWebContentsDir(), template, msg,
				theme, !productionMode,
				heartBeat, uiPath);
	}
}
