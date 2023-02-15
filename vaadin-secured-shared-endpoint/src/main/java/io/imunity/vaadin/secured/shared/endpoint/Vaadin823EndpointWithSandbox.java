/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.secured.shared.endpoint;

import com.vaadin.flow.server.VaadinServlet;
import io.imunity.vaadin.endpoint.common.CustomResourceProvider;
import io.imunity.vaadin.endpoint.common.Vaadin823Endpoint;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.UnityBootstrapHandler;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;
import pl.edu.icm.unity.webui.sandbox.AccountAssociationSandboxUI;

import static pl.edu.icm.unity.webui.VaadinEndpoint.DEFAULT_HEARTBEAT;
import static pl.edu.icm.unity.webui.VaadinEndpoint.SANDBOX_PATH_ASSOCIATION;

public class Vaadin823EndpointWithSandbox extends Vaadin823Endpoint
{
	public Vaadin823EndpointWithSandbox(NetworkServer server, AdvertisedAddressProvider advertisedAddrProvider,
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
		SandboxAuthnRouter sandboxRouter = new SandboxAuthnRouterImplV23();
		Vaadin23WebAppContextWithSandbox webAppContext = new Vaadin23WebAppContextWithSandbox(properties, genericEndpointProperties, msg, description, sandboxRouter);
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

	private int getHeartbeatInterval(int sessionTimeout)
	{
		if (sessionTimeout >= 3*DEFAULT_HEARTBEAT)
			return DEFAULT_HEARTBEAT;
		int ret = sessionTimeout/3;
		return Math.max(ret, 2);
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
