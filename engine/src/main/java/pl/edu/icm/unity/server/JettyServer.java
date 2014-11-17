/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
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
	
	@Autowired
	public JettyServer(UnityServerConfiguration cfg, PKIManagement pkiManagement)
	{
		super(createURLs(cfg.getJettyProperties()), pkiManagement.getMainAuthnAndTrust(), 
				cfg.getJettyProperties(), null);
		initServer();
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

	/**
	 * Deploys a classic Unity endpoint.
	 * @param endpoint
	 * @throws EngineException
	 */
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
		
		mainContextHandler.addHandler(handler);
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
		
		ServletContextHandler handler = usedContextPaths.get(endpoint.getEndpointDescription().getContextAddress());
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
	
	public synchronized void undeployAllEndpoints() throws EngineException
	{
		Set<String> ids = new HashSet<String>();
		
		for (WebAppEndpointInstance webapp: deployedEndpoints)
			ids.add(webapp.getEndpointDescription().getId());
			
		for (String id: ids)
			undeployEndpoint(id);
	}
	
	public synchronized List<WebAppEndpointInstance> getDeployedEndpoints()
	{
		return new ArrayList<WebAppEndpointInstance>(deployedEndpoints);
	}
	
	@Override
	public URL getAdvertisedAddress()
	{
		String advertisedHost = extraSettings.getValue(UnityHttpServerConfiguration.ADVERTISED_HOST);
		if (advertisedHost == null)
			return getUrls()[0];
		URL url = getUrls()[0];
		try {
			return new URL(url.getProtocol(), advertisedHost, 
					url.getPort(), url.getFile());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Ups, URL can not " +
					"be reconstructed, while it should", e);
		}
	}
}









