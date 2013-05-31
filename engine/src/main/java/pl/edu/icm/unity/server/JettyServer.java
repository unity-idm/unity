/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.jetty.JettyServerBase;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityHttpServerConfiguration;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

/**
 * Manages HTTP server. Mostly responsible for creating proper hierarchy of HTTP handlers for deployed
 * {@link WebAppEndpointInstance} instances.
 * <p>
 * Jetty structure which is used:
 *  {@link ContextHandlerCollection} is used to manage all deployed contexts (fixed, one instance)
 *  Endpoints provide a single {@link ServletContextHandler} which describes an endpoint's web application.
 * 
 * @author K. Benedyczak
 */
@Component
public class JettyServer extends JettyServerBase implements Lifecycle
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, UnityApplication.class);
	private List<WebAppEndpointInstance> deployedEndpoints;
	private Set<String> usedContextPaths;
	
	@Autowired
	public JettyServer(UnityServerConfiguration cfg)
	{
		super(createURLs(cfg.getJettyProperties()), cfg.getAuthAndTrust(), cfg.getJettyProperties(), null);
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
		usedContextPaths = new HashSet<String>();
		ContextHandlerCollection handlersCollection = new ContextHandlerCollection();
		deployedEndpoints = new ArrayList<WebAppEndpointInstance>(16);
		//TODO a custom default handler is needed
		handlersCollection.addHandler(new DefaultHandler());
		return handlersCollection;
	}
	
	public synchronized void deployEndpoint(WebAppEndpointInstance endpoint) 
			throws EngineException
	{
		ServletContextHandler handler = endpoint.getServletContextHandler(); 
		String contextPath = handler.getContextPath();
		if (usedContextPaths.contains(contextPath))
		{
			throw new WrongArgumentException("There are (at least) two web " +
					"applications configured at the same context path: " + contextPath);
		}
		
		ContextHandlerCollection root = (ContextHandlerCollection) getRootHandler();
		root.addHandler(handler);
		try
		{
			handler.start();
		} catch (Exception e)
		{
			root.removeHandler(handler);
			throw new EngineException("Can not start handler", e);
		}
		usedContextPaths.add(contextPath);
		deployedEndpoints.add(endpoint);
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
		
		ServletContextHandler handler = endpoint.getServletContextHandler();
		try
		{
			handler.stop();
		} catch (Exception e)
		{
			throw new EngineException("Can not stop handler", e);
		}
		ContextHandlerCollection root = (ContextHandlerCollection) getRootHandler();
		root.removeHandler(handler);
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
		List<WebAppEndpointInstance> ret = new ArrayList<WebAppEndpointInstance>(deployedEndpoints.size());
		ret.addAll(deployedEndpoints);
		return ret;
	}
}









