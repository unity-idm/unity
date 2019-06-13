/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.jetty.PlainServerConnector;
import pl.edu.icm.unity.base.utils.Log;


/*
 * Issues:
 * -) no support for HTTP2 (yet - we may implement it, won't be super easy...)
 * -) added latency (very small: when Unity proxy and backend server are all on same machine, added latency is ca 3ms)
 * -) no SSO for multi-domain setups i.e. SSO only across servers behind the same Terminal. We can work on 
 * implementing SSO across multiple Terminals or even including the main Unity -> but only if terminals/Unity are all 
 * in the same common domain (e.g. app.acme.com and signin.app.acme.com). Would require to allow for using a custom
 * domain for the cloud unity service and unification of cookies.
 * 
 * Howtos:
 * -) get authenticated user info: persistent-user-id always in X-Unity-UserId header. As additional feature we may add
 * additional headers X-Unity-YOURNAME=identity or attribute, so that basic info is available without external call.
 * Regular authn of app.
 * 
 * -) logout user: remove the authn cookie. UTProxy may monitor for authentication cookie removal and remove the 
 * stored token too as an additional security measure (not critical)
 * 
 * -) check session status (e.g. to know when it is expiring): X-Unity-SessionExpiration
 * 
 * -) configuration of Unity Terminal: security token, client id, URL of base Unity instance id, 
 * local server certificate&key, listen address, port, TLS mode on/off, thread pool.
 * -) remaining configuration in main Unity, in endpoint serving the Terminal: paths mappings, attributes added in 
 * headers etc. Terminal polls main Unity for configuration file, fetching it only when changed. 
 * Stored as JSON file locally.
 * 
 * 
 * TODO:
 * 
 * - actual checking whether token from cookie is valid
 * 
 * - code reuse with main Unity server stack
 * -- TLS configuration for the proxy server
 * -- logging configuration
 * -- proper filtering: only GETs are redirected, deny other unauthenticated methods
 * -- setup proper error page needed handlers etc.
 * 
 * - no session at all and expiring map with states as keys. 
 * -- preserve and restore the original request URL after successful authentication
 * -- clear state after successful/failed authn
 * 
 * - Oauth cleaning
 * -- token exchange, fetching profile
 * -- setting of local cached user info in proxied request
 * -- refreshing of user's info
 * 
 * - lifecycle management of authn cookie: 
 * -- record usage and expire too old
 * -- advertise session time to backend.
 * 
 * - endpoint in Unity providing configuration with UI. 
 * - configuration of path mappings, and which of the paths require authN. Consider flexible setup: controleable default 
 * either secured or public paths enumeration. Consider supporting virtual hosts.
 * - download of configuration and its updates and application
 *  
 * - support for headless clients and Authorization header (API gateway). 
 */
@Component
public class JettyServer implements Lifecycle
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, JettyServer.class);
	private Server theServer;
	private final AuthenticationCheckingFilter authnFilter;
	private final OAuthAuthenticationServlet authnServlet;

	@Autowired
	JettyServer(AuthenticationCheckingFilter authnFilter, OAuthAuthenticationServlet authnServlet)
	{
		this.authnFilter = authnFilter;
		this.authnServlet = authnServlet;
		try
		{
			initServer();
		} catch (MalformedURLException e)
		{
			throw new RuntimeException("Error initializing proxy", e);
		}
	}

	private void initServer() throws MalformedURLException
	{
		theServer = createServer();

		Connector[] connectors = createConnectors();
		for (Connector connector : connectors)
		{
			theServer.addConnector(connector);
		}

		initRootHandler();
	}

	@Override
	public void start()
	{
	        try
		{
	        	log.debug("Starting Jetty HTTP server");
			theServer.start();
			log.info("Jetty HTTP server was started");
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
			log.debug("Stopping Jetty HTTP server");
			theServer.stop();
			log.info("Jetty HTTP server was stopped");
		} catch (Exception e)
		{
			log.error("Problem stopping HTTP Jetty server: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isRunning()
	{
		return (theServer == null) ? false : theServer.isRunning();
	}

	
	private Server createServer()
	{
		return new Server(getThreadPool());
	}

	private QueuedThreadPool getThreadPool()
	{
		QueuedThreadPool btPool = new QueuedThreadPool();
		int extraThreads = 3;
		btPool.setMaxThreads(10 + extraThreads);
		btPool.setMinThreads(10 + extraThreads);
		return btPool;
	}

	
	private Connector[] createConnectors() throws MalformedURLException
	{
		URL url = new URL("https://0.0.0.0:8000");
		ServerConnector plainConnector = createPlainConnector(url);
		configureConnector(plainConnector, url);
		return new Connector[] {plainConnector};
	}
	
	private ServerConnector getPlainConnectorInstance()
	{
		HttpConnectionFactory httpConnFactory = getHttpConnectionFactory();
		return new PlainServerConnector(theServer, httpConnFactory);
	}

	/**
	 * By default http connection factory is configured not to send server identification data.
	 */
	private HttpConnectionFactory getHttpConnectionFactory()
	{
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		httpConfig.setSendXPoweredBy(false);
		return new HttpConnectionFactory(httpConfig);
	}

	/**
	 * Creates an insecure connector and configures it.
	 */
	private ServerConnector createPlainConnector(URL url)
	{
		log.debug("Creating plain HTTP connector on: " + url);
		return getPlainConnectorInstance();
	}

	/**
	 * Sets parameters on the Connector, which are shared by all of them regardless of their type. 
	 * The default implementation sets port and hostname.
	 */
	private void configureConnector(ServerConnector connector, URL url) throws ConfigurationException
	{
		connector.setHost(url.getHost());
		connector.setPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
		connector.setIdleTimeout(3600_000);
	}


	private FilterHolder getAuthnCheckingFilter()
	{
		return new FilterHolder(authnFilter);
	}
	
	private synchronized void initRootHandler()
	{
		ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		ProxyServlet proxy = new AuthnInjectingProxyServlet();
		ServletHolder proxyServlet = new ServletHolder(proxy);
		contextHandler.addFilter(getAuthnCheckingFilter(), "/*", EnumSet.of(DispatcherType.REQUEST));
		proxyServlet.setInitParameter("proxyTo", "http://127.0.0.1:8001");
		proxyServlet.setInitParameter("preserveHost", "true");
		contextHandler.addServlet(proxyServlet, "/*");
		
		ServletHolder authnServlet = new ServletHolder(this.authnServlet);
		contextHandler.addServlet(authnServlet, AuthenticationCheckingFilter.INTERNAL_AUTHN_PATH);
		
		theServer.setHandler(contextHandler);
		try
		{
			contextHandler.start();
		} catch (Exception e)
		{
			throw new RuntimeException("Can not start handler", e);
		}
	}

}
