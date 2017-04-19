/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.DoSFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityHttpServerConfiguration;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.jetty.JettyDefaultHandler;
import eu.unicore.util.jetty.JettyServerBase;

/**
 * Manages HTTP server. Mostly responsible for creating proper hierarchy of HTTP handlers for deployed
 * {@link WebAppEndpointInstance} instances.
 * <p>
 * Jetty structure which is used:
 *  {@link ContextHandlerCollection} is used to manage all deployed contexts (fixed, one instance)
 *  Endpoints provide a single {@link ServletContextHandler} which describes an endpoint's web application.
 * <p>
 *  If needed it is wrapped in some rewrite handler.
 * @author K. Benedyczak
 */
@Component
public class JettyServer extends JettyServerBase implements Lifecycle, NetworkServer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, UnityApplication.class);
	private List<WebAppEndpointInstance> deployedEndpoints;
	private Map<String, ServletContextHandler> usedContextPaths;
	private ContextHandlerCollection mainContextHandler;
	private FilterHolder dosFilter = null;
	
	@Autowired
	public JettyServer(UnityServerConfiguration cfg, PKIManagement pkiManagement)
	{
		super(createURLs(cfg.getJettyProperties()), pkiManagement.getMainAuthnAndTrust(), 
				cfg.getJettyProperties(), null);
		initServer();
		dosFilter = getDOSFilter();
		addRedirectHandler(cfg);
	}

	private static URL[] createURLs(UnityHttpServerConfiguration conf)
	{
		try
		{
			return new URL[] {new URL("https://" + conf.getValue(UnityHttpServerConfiguration.HTTPS_HOST) + 
					":" + conf.getValue(UnityHttpServerConfiguration.HTTPS_PORT))};
		} catch (MalformedURLException e)
		{
			throw new ConfigurationException("Can not create server url from host and port parameters: " 
					+ e.getMessage(), e);
		}
	}

	@Override
	public void start()
	{
	        try
		{
			super.start();
		} catch (Exception e)
		{
			log.error("Problem starting HTTP Jetty server: " + e.getMessage(), e);
		}
	}

	@Override
	public void stop()
	{
		try
		{
			super.stop();
		} catch (Exception e)
		{
			log.error("Problem stopping HTTP Jetty server: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isRunning()
	{
		return (getServer() == null) ? false : getServer().isRunning();
	}

	@Override
	protected synchronized Handler createRootHandler() throws ConfigurationException
	{
		usedContextPaths = new HashMap<String, ServletContextHandler>();
		mainContextHandler = new ContextHandlerCollection();
		deployedEndpoints = new ArrayList<WebAppEndpointInstance>(16);
		mainContextHandler.addHandler(new JettyDefaultHandler());
		return mainContextHandler;
	}

	private void addRedirectHandler(UnityServerConfiguration cfg) throws ConfigurationException
	{
		if (cfg.isSet(UnityServerConfiguration.DEFAULT_WEB_PATH))
		{
			try
			{
				deployHandler(new RedirectHandler(cfg.getValue(
						UnityServerConfiguration.DEFAULT_WEB_PATH)));
			} catch (EngineException e)
			{
				log.error("Cannot deploy redirect handler " + e.getMessage(), e);
			}
		}

	}

	/**
	 * Deploys a classic Unity endpoint.
	 * @param endpoint
	 * @throws EngineException
	 */
	@Override
	public synchronized void deployEndpoint(WebAppEndpointInstance endpoint) 
			throws EngineException
	{
		ServletContextHandler handler = endpoint.getServletContextHandler();
		deployHandler(handler);
		deployedEndpoints.add(endpoint);
	}
	
	/**
	 * Deploys a simple handler. It is only checked if the context path is free.
	 * @param handler
	 * @throws EngineException
	 */
	public synchronized void deployHandler(ServletContextHandler handler) 
			throws EngineException
	{
		String contextPath = handler.getContextPath();
		if (usedContextPaths.containsKey(contextPath))
		{
			throw new WrongArgumentException("There are (at least) two web " +
					"applications configured at the same context path: " + contextPath);
		}
		
		handler.setServer(getServer());
		mainContextHandler.addHandler(handler);
		configureGzip(handler);
		if (dosFilter != null)
		{
			log.info("Enabling DoS filter on context " + handler.getContextPath());
			handler.addFilter(dosFilter, "/*", EnumSet.of(DispatcherType.REQUEST));
		}
		
		try
		{
			handler.start();
		} catch (Exception e)
		{
			mainContextHandler.removeHandler(handler);
			throw new EngineException("Can not start handler", e);
		}
		usedContextPaths.put(contextPath, handler);
	}
	
	@Override
	public synchronized void undeployEndpoint(String id) throws EngineException
	{
		WebAppEndpointInstance endpoint = null;
		for (WebAppEndpointInstance endp: deployedEndpoints)
			if (endp.getEndpointDescription().getId().equals(id))
			{
				endpoint = endp;
				break;
			}
		if (endpoint == null)
			throw new WrongArgumentException("There is no deployed endpoint with id " + id);
		
		ServletContextHandler handler = usedContextPaths.get(
				endpoint.getEndpointDescription().getContextAddress());
		try
		{
			handler.stop();
		} catch (Exception e)
		{
			throw new EngineException("Can not stop handler", e);
		}
		mainContextHandler.removeHandler(handler);
		usedContextPaths.remove(handler.getContextPath());
		deployedEndpoints.remove(endpoint);
	}
	
	@Override
	public URL getAdvertisedAddress()
	{
		String advertisedHost = extraSettings.getValue(UnityHttpServerConfiguration.ADVERTISED_HOST);
		if (advertisedHost == null)
			return getUrls()[0];
		
		try {
			return new URL("https://" + advertisedHost);
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Ups, URL can not " +
					"be reconstructed, while it should", e);
		}
	}
	
	private FilterHolder getDOSFilter()
	{
		if (!extraSettings.getBooleanValue(UnityHttpServerConfiguration.ENABLE_DOS_FILTER))
			return null;
		FilterHolder holder = new FilterHolder(new DoSFilter());
		UnityHttpServerConfiguration conf = (UnityHttpServerConfiguration)extraSettings;
		Set<String> keys = conf.
				getSortedStringKeys(UnityHttpServerConfiguration.DOS_FILTER_PFX, false);
		for (String key: keys)
			holder.setInitParameter(key.substring(
						UnityHttpServerConfiguration.PREFIX.length() + 
						UnityHttpServerConfiguration.DOS_FILTER_PFX.length()), 
					conf.getProperty(key));
		return holder;
	}
}









