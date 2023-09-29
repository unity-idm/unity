/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import org.eclipse.jetty.ee8.servlet.FilterHolder;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.URLResourceFactory;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilterV8;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnRouterImpl;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;

public class InsecureVaadinEndpoint extends VaadinEndpoint
{
	public InsecureVaadinEndpoint(NetworkServer server, AdvertisedAddressProvider advertisedAddrProvider,
			MessageSource msg, ApplicationContext applicationContext, String uiBeanName, String servletPath,
			RemoteRedirectedAuthnResponseProcessingFilterV8 remoteAuthnResponseProcessingFilter)
	{
		super(server, advertisedAddrProvider, msg, applicationContext, uiBeanName, servletPath,
				remoteAuthnResponseProcessingFilter);
	}

	protected ServletContextHandler getServletContextHandlerOverridable()
	{
		if (context != null)
			return context;

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getEndpoint().getContextAddress());
		
		context.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST));
		
		contextSetupFilter = new InvocationContextSetupFilter(serverConfig, description.getRealm(),
				getServletUrl(uiServletPath), getAuthenticationFlows());
		context.addFilter(new FilterHolder(contextSetupFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
			
		UnityBootstrapHandler handler4Main = getBootstrapHandlerGeneric(uiServletPath, LONG_HEARTBEAT,
				genericEndpointProperties.getEffectiveMainTheme());
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticationFlows, null, properties,
				handler4Main);
		ServletHolder vaadinServletHolder = createVaadinServletHolder(theServlet, true);
		context.addServlet(vaadinServletHolder, VAADIN_RESOURCES);
		context.addServlet(vaadinServletHolder, uiServletPath + "/*");
		context.addServlet(new ServletHolder(new ForwadSerlvet()), "/*");
		return context;
	}

	@Override
	public final synchronized ServletContextHandler getServletContextHandler()
	{
		context = getServletContextHandlerOverridable();
		String webContentDir = getWebContentsDir();
		SandboxAuthnRouter sandboxRouter = new SandboxAuthnRouterImpl();
		theServlet.setSandboxRouter(sandboxRouter);		
		if (webContentDir != null)
			context.setBaseResource(new URLResourceFactory().newResource(webContentDir));
		return context;
	}
	
	@Override
	public final void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		throw new UnsupportedOperationException();
	}
}
