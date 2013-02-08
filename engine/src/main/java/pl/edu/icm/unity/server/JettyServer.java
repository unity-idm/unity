/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
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

import pl.edu.icm.unity.server.provider.WebApplicationProvider;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityHttpServerConfiguration;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

/**
 * Manages HTTP server. Mostly responsible for creating proper hierarchy of HTTP handlers for injected
 * {@link WebApplicationProvider} instances.
 * @author K. Benedyczak
 */
@Component
public class JettyServer extends JettyServerBase implements Lifecycle
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, UnityApplication.class);
	
	private WebApplicationProvider[] webapps;
	
	@Autowired(required=false)
	public JettyServer(UnityServerConfiguration cfg)
	{
		this(cfg, new WebApplicationProvider[0]);
	}
	
	@Autowired(required=false)
	public JettyServer(UnityServerConfiguration cfg, WebApplicationProvider[] webapps)
	{
		super(createURLs(cfg.getJettyProperties()), cfg.getAuthAndTrust(), cfg.getJettyProperties(), null);
		this.webapps = webapps;
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
	protected Handler createRootHandler() throws ConfigurationException
	{
		Set<String> usedContextPaths = new HashSet<String>();
		ContextHandlerCollection handlersCollection = new ContextHandlerCollection();
		for (WebApplicationProvider provider: webapps)
			for (ServletContextHandler handler: provider.getServletContextHandlers())
			{
				if (!usedContextPaths.add(handler.getContextPath()))
				{
					throw new ConfigurationException("There are (at least) two web " +
							"applications configured at the same context path: " + handler.getContextPath());
				}
				handlersCollection.addHandler(handler);
			}
		if (!usedContextPaths.contains("/"))
			handlersCollection.addHandler(new DefaultHandler());
		return handlersCollection;
	}
}
