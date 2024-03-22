/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.jetty.SecuredServerConnector;
import jakarta.servlet.ServletException;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.http.UriCompliance.Violation;
import org.eclipse.jetty.rewrite.handler.HeaderPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.SessionIdManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityHttpServerConfiguration;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.server.NetworkServer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static pl.edu.icm.unity.engine.api.config.UnityHttpServerConfiguration.*;

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
public class JettyServer implements Lifecycle, NetworkServer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, UnityApplication.class);
	private List<WebAppEndpointInstance> deployedEndpoints;
	private Map<String, org.eclipse.jetty.server.Handler> usedContextPaths;
	private ContextHandlerCollection mainContextHandler;
	private FilterHolder dosFilter = null;
	private final String defaultWebContentsPath;

	private final URL[] listenUrls;
	private final IAuthnAndTrustConfiguration securityConfiguration;
	private final UnityHttpServerConfiguration serverSettings;

	private Server theServer;

	@Autowired
	public JettyServer(UnityServerConfiguration cfg, PKIManagement pkiManagement,
			ListeningUrlsProvider listenUrlsProvider)
	{
		this(cfg.getJettyProperties(), 
				cfg.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH), 
				pkiManagement.getMainAuthnAndTrust(), 
				listenUrlsProvider.getListenUrls());
	}

	JettyServer(UnityHttpServerConfiguration serverSettings,
			String defaultWebContentsPath, 
			IAuthnAndTrustConfiguration securityConfiguration,
			URL[] listenUrls)
	{
		this.securityConfiguration = securityConfiguration;
		this.listenUrls = listenUrls;
		this.serverSettings = serverSettings;
		this.defaultWebContentsPath = defaultWebContentsPath;
		initServer();
		dosFilter = createDoSFilterInstance();
	}
	
	@Override
	public void start()
	{
	        try
		{
	        	log.debug("Starting Jetty HTTP server");
			theServer.start();
			updatePortsIfNeeded();
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


	/**
	 * Invoked after server is started: updates the listen URLs with the actual port,
	 * if originally it was set to 0, what means that server should choose a random one
	 */
	private void updatePortsIfNeeded()
	{
		Connector[] conns = theServer.getConnectors();

		for (int i=0; i<listenUrls.length; i++)
		{
			URL url = listenUrls[i];
			if (url.getPort() == 0)
			{
				int port = ((NetworkConnector)conns[i]).getLocalPort();
				try
				{
					listenUrls[i] = new URL(url.getProtocol(),
							url.getHost(), port, url.getFile());
				} catch (MalformedURLException e)
				{
					throw new RuntimeException("Ups, URL can not " +
							"be reconstructed, while it should", e);
				}
			}
		}
	}


	private void initServer() throws ConfigurationException
	{
		if (listenUrls.length == 1 && "0.0.0.0".equals(listenUrls[0].getHost()))
		{
			log.info("Creating Jetty HTTP server, will listen on all network interfaces");
		} else
		{
			StringBuilder allAddresses = new StringBuilder();
			for (URL url : listenUrls)
				allAddresses.append(url).append(" ");
			log.info("Creating Jetty HTTP server, will listen on: " + allAddresses);
		}

		theServer = createServer();

		if (serverSettings.getBooleanValue(UnityHttpServerConfiguration.FAST_RANDOM))
			configureFastAndInsecureSessionIdGenerator();

		Connector[] connectors = createConnectors();
		for (Connector connector : connectors)
			theServer.addConnector(connector);

		initRootHandler();

		Handler handler = configureHsts(mainContextHandler);
		handler = configureFrameOptions(handler);
		handler = configureGzip(handler);
		handler = new TraceBlockingHandler(handler);

		theServer.setHandler(handler);
		configureErrorHandler();
	}

	private Server createServer()
	{
		return new Server(getThreadPool());
	}

	private QueuedThreadPool getThreadPool()
	{
		QueuedThreadPool btPool = new QueuedThreadPool();
		int extraThreads = listenUrls.length * 3;
		btPool.setMaxThreads(serverSettings.getIntValue(UnityHttpServerConfiguration.MAX_THREADS) + extraThreads);
		btPool.setMinThreads(serverSettings.getIntValue(UnityHttpServerConfiguration.MIN_THREADS) + extraThreads);
		return btPool;
	}

	private Handler configureFrameOptions(Handler toWrap)
	{
		XFrameOptions frameOpts = serverSettings.getEnumValue(UnityHttpServerConfiguration.FRAME_OPTIONS,
				XFrameOptions.class);
		if (frameOpts != XFrameOptions.allow)
		{
			RewriteHandler rewriter = new RewriteHandler(toWrap);
			HeaderPatternRule frameOriginRule = new HeaderPatternRule();
			frameOriginRule.setHeaderName("X-Frame-Options");

			StringBuilder sb = new StringBuilder(frameOpts.toHttp());
			if (frameOpts == XFrameOptions.allowFrom)
			{
				String allowedOrigin = serverSettings.getValue(UnityHttpServerConfiguration.ALLOWED_TO_EMBED);
				sb.append(" ").append(allowedOrigin);
			}
			frameOriginRule.setHeaderValue(sb.toString());
			frameOriginRule.setPattern("*");
			rewriter.addRule(frameOriginRule);
			return rewriter;
		}
		return toWrap;
	}

	private Handler configureHsts(Handler toWrap)
	{
		if (serverSettings.getBooleanValue(UnityHttpServerConfiguration.ENABLE_HSTS))
		{
			RewriteHandler rewriter = new RewriteHandler(toWrap);
			HeaderPatternRule hstsRule = new HeaderPatternRule();
			hstsRule.setHeaderName(HttpHeader.STRICT_TRANSPORT_SECURITY.asString());
			hstsRule.setHeaderValue("max-age=31536000; includeSubDomains");
			hstsRule.setPattern("*");
			rewriter.addRule(hstsRule);
			return rewriter;
		}
		return toWrap;
	}


	private void configureFastAndInsecureSessionIdGenerator()
	{
		log.info("Using fast (but less secure) session ID generator");
		SessionIdManager sm = new DefaultSessionIdManager(theServer, new java.util.Random());
		theServer.addBean(sm);
	}

	private Connector[] createConnectors() throws ConfigurationException
	{
		ServerConnector[] ret = new ServerConnector[listenUrls.length];
		for (int i = 0; i < listenUrls.length; i++)
		{
			ret[i] = createConnector(listenUrls[i]);
			configureConnector(ret[i], listenUrls[i]);
		}
		return ret;
	}

	/**
	 * Default connector creation: uses {@link #createSecureConnector(URL)}
	 * and {@link #createPlainConnector(URL)} depending on the URL protocol.
	 * Returns a fully configured connector.
	 */
	private ServerConnector createConnector(URL url) throws ConfigurationException
	{
		return url.getProtocol().startsWith("https") ?
			createSecureConnector(url) : createPlainConnector(url);
	}

	/**
	 * @return an instance of NIO secure connector. It uses proper
	 *         validators and credentials and lowResourcesConnections are
	 *         set to the difference between MAX and LOW THREADS.
	 */
	protected SecuredServerConnector getSecuredConnectorInstance() throws ConfigurationException
	{
		HttpConnectionFactory httpConnFactory = getHttpConnectionFactory();
		SslContextFactory.Server secureContextFactory;
		try
		{
			secureContextFactory = SecuredServerConnector.createContextFactory(
					securityConfiguration.getValidator(), securityConfiguration.getCredential());
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't create secure context factory", e);
		}
		return new SecuredServerConnector(theServer, secureContextFactory, httpConnFactory);
	}

	/**
	 * Creates a secure connector and configures it with security-related settings.
	 */
	private ServerConnector createSecureConnector(URL url) throws ConfigurationException
	{
		log.debug("Creating SSL NIO connector on: " + url);
		SecuredServerConnector ssl = getSecuredConnectorInstance();

		SslContextFactory.Server factory = ssl.getSslContextFactory();
		factory.setNeedClientAuth(serverSettings.getBooleanValue(UnityHttpServerConfiguration.REQUIRE_CLIENT_AUTHN));
		factory.setWantClientAuth(serverSettings.getBooleanValue(UnityHttpServerConfiguration.WANT_CLIENT_AUTHN));

		String disabledProtocols = serverSettings.getValue(UnityHttpServerConfiguration.DISABLED_PROTOCOLS);
		if (disabledProtocols != null)
		{
			disabledProtocols = disabledProtocols.trim();
			factory.setExcludeProtocols(disabledProtocols.split("[ ]+"));
		}
		String disabledCiphers = serverSettings.getValue(UnityHttpServerConfiguration.DISABLED_CIPHER_SUITES);
		if (disabledCiphers != null)
		{
			disabledCiphers = disabledCiphers.trim();
			if (disabledCiphers.length() > 1)
				factory.setExcludeCipherSuites(disabledCiphers.split("[ ]+"));
		}
		log.info("SSL protocol was set to: '" + factory.getProtocol() + "'");
		return ssl;
	}

	/**
	 * @return an instance of insecure connector. It is only configured not
	 *         to send server version and supports connections logging.
	 */
	private ServerConnector getPlainConnectorInstance()
	{
		HttpConnectionFactory httpConnFactory = getHttpConnectionFactory();
		return new ServerConnector(theServer, httpConnFactory);
	}

	/**
	 * By default http connection factory is configured not to send server identification data.
	 */
	private HttpConnectionFactory getHttpConnectionFactory()
	{
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		httpConfig.setSendXPoweredBy(false);
		httpConfig.setUriCompliance(new UriCompliance("allowForEncodedPathSeparators",
			        Set.of(Violation.AMBIGUOUS_PATH_SEPARATOR)));
		SecureRequestCustomizer src = new SecureRequestCustomizer();
		src.setSniHostCheck(serverSettings.getBooleanValue(SNI_HOSTNAME_CHECK));
		httpConfig.addCustomizer(src);
		return new HttpConnectionFactory(httpConfig);
	}

	/**
	 * Creates an insecure connector and configures it.
	 */
	private ServerConnector createPlainConnector(URL url)
	{
		log.info("Creating plain HTTP connector on: " + url);
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
		connector.setIdleTimeout(serverSettings.getIntValue(UnityHttpServerConfiguration.MAX_IDLE_TIME));
	}

	/**
	 * Configures Gzip filter if gzipping is enabled, for all servlet
	 * handlers which are configured. Warning: if you use a complex setup of
	 * handlers it might be better to override this method and enable
	 * compression selectively.
	 */
	private Handler configureGzip(Handler handler) throws ConfigurationException
	{
		boolean enableGzip = serverSettings.getBooleanValue(UnityHttpServerConfiguration.ENABLE_GZIP);
		if (enableGzip)
		{
			GzipHandler gzipHandler = new GzipHandler();
			gzipHandler.setMinGzipSize(serverSettings.getIntValue(UnityHttpServerConfiguration.MIN_GZIP_SIZE));
			log.info("Enabling GZIP compression filter");
			gzipHandler.setServer(theServer);
			gzipHandler.setHandler(handler);
			return gzipHandler;
		} else
			return handler;
	}

	/**
	 * @return array of URLs where the server is listening
	 */
	public URL[] getUrls()
	{
		return listenUrls;
	}

	private void configureErrorHandler()
	{
		theServer.setErrorHandler(new JettyErrorHandler(defaultWebContentsPath));
	}

	@Override
	public boolean isRunning()
	{
		return (theServer == null) ? false : theServer.isRunning();
	}

	private synchronized void initRootHandler()
	{
		usedContextPaths = new HashMap<>();
		mainContextHandler = new ContextHandlerCollection();
		deployedEndpoints = new ArrayList<>(16);
	}

	/**
	 * Deploys a classic Unity endpoint.
	 */
	@Override
	public synchronized void deployEndpoint(WebAppEndpointInstance endpoint)
			throws EngineException
	{
		org.eclipse.jetty.ee10.servlet.ServletContextHandler handler = endpoint.getServletContextHandler();
		handler.getServletHandler().setDecodeAmbiguousURIs(true);
		deployHandler(handler, endpoint.getEndpointDescription().getName());
		deployedEndpoints.add(endpoint);
	}

	@Override
	public synchronized void deployHandler(ServletContextHandler handler, String endpointId)
			throws EngineException
	{
		String contextPath = handler.getContextPath();
		if (usedContextPaths.containsKey(contextPath))
		{
			throw new WrongArgumentException("There are (at least) two web " +
					"applications configured at the same context path: " + contextPath);
		}

		addDoSFilter(handler);
		addCORSFilter(handler);

		ClientIPSettingHandler clientIPSettingHandler = applyClientIPDiscoveryHandler(handler, endpointId);
		mainContextHandler.addHandler(clientIPSettingHandler);
		if(theServer.isStarted())
		{
			try
			{
				clientIPSettingHandler.start();
			} catch (Exception e)
			{
				mainContextHandler.removeHandler(clientIPSettingHandler);
				throw new EngineException("Can not start handler", e);
			}
		}
		usedContextPaths.put(contextPath, clientIPSettingHandler);
	}

	@Override
	public synchronized void undeployAllHandlers() throws EngineException
	{
		for (org.eclipse.jetty.server.Handler handler : usedContextPaths.values())
		{
			try
			{
				handler.stop();
			} catch (Exception e)
			{
				throw new EngineException("Can not stop handler", e);
			}
		}
		usedContextPaths.clear();

		for (org.eclipse.jetty.server.Handler handler : List.copyOf(mainContextHandler.getHandlers()))
		{
			mainContextHandler.removeHandler(handler);
		}
	}

	@Override
	public synchronized void undeployHandler(String contextPath) throws EngineException
	{
		org.eclipse.jetty.server.Handler handler = usedContextPaths.get(contextPath);
		try
		{
			handler.stop();
		} catch (Exception e)
		{
			throw new EngineException("Can not stop handler", e);
		}
		mainContextHandler.removeHandler(handler);
		usedContextPaths.remove(contextPath);
	}

	@Override
	public synchronized void undeployEndpoint(String id) throws EngineException
	{
		if(deployedEndpoints.stream().anyMatch(endp -> endp.getEndpointDescription().getName().equals(id)))
			undeployEE10Endpoint(id);
		else
			throw new WrongArgumentException("There is no deployed endpoint with id " + id);
	}

	public void undeployEE10Endpoint(String id) throws EngineException
	{
		WebAppEndpointInstance endpoint = null;
		for (WebAppEndpointInstance endp: deployedEndpoints)
		{
			if (endp.getEndpointDescription().getName().equals(id))
			{
				endpoint = endp;
				break;
			}
		}
		if (endpoint == null)
			throw new WrongArgumentException("There is no deployed endpoint with id " + id);

		String contextPath = endpoint.getEndpointDescription().getEndpoint().getContextAddress();
		org.eclipse.jetty.server.Handler handler = usedContextPaths.get(contextPath);
		try
		{
			handler.stop();
		} catch (Exception e)
		{
			throw new EngineException("Can not stop handler", e);
		}
		mainContextHandler.removeHandler(handler);
		usedContextPaths.remove(contextPath);
		deployedEndpoints.remove(endpoint);
	}

	@Override
	public Set<String> getUsedContextPaths()
	{
		return usedContextPaths.keySet();
	}

	private org.eclipse.jetty.ee10.servlet.FilterHolder createDoSFilterInstance()
	{
		if (!serverSettings.getBooleanValue(UnityHttpServerConfiguration.ENABLE_DOS_FILTER))
			return null;
		org.eclipse.jetty.ee10.servlet.FilterHolder holder = new org.eclipse.jetty.ee10.servlet.FilterHolder(new org.eclipse.jetty.ee10.servlets.DoSFilter());
		Set<String> keys = serverSettings.getSortedStringKeys(UnityHttpServerConfiguration.DOS_FILTER_PFX);
		for (String key: keys)
			holder.setInitParameter(key.substring(
							UnityHttpServerConfiguration.PREFIX.length() +
									UnityHttpServerConfiguration.DOS_FILTER_PFX.length()),
					serverSettings.getProperty(key));
		return holder;
	}

	private void addDoSFilter(ServletContextHandler handler)
	{
		if (dosFilter != null)
		{
			log.info("Enabling DoS filter on context " + handler.getContextPath());
			handler.addFilter(dosFilter, "/*", EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));
		}
	}

	private void addCORSFilter(ServletContextHandler handler)
	{
		boolean enable = serverSettings.getBooleanValue(UnityHttpServerConfiguration.ENABLE_CORS);
		if (!enable)
			return;

		log.info("Enabling CORS");
		org.eclipse.jetty.ee10.servlets.CrossOriginFilter cors = new org.eclipse.jetty.ee10.servlets.CrossOriginFilter();
		jakarta.servlet.FilterConfig config = new jakarta.servlet.FilterConfig()
		{

			@Override
			public jakarta.servlet.ServletContext getServletContext()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public Enumeration<String> getInitParameterNames()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public String getInitParameter(String name)
			{
				return serverSettings.getValue(UnityHttpServerConfiguration.CORS_PFX + name);
			}

			@Override
			public String getFilterName()
			{
				return "CORS";
			}
		};
		try
		{
			cors.init(config);
		} catch (ServletException e)
		{
			throw new ConfigurationException("Error setting up CORS", e);
		}
		org.eclipse.jetty.ee10.servlet.FilterHolder filterHolder = new org.eclipse.jetty.ee10.servlet.FilterHolder(cors);
		handler.addFilter(filterHolder, "/*", EnumSet.of(jakarta.servlet.DispatcherType.REQUEST, jakarta.servlet.DispatcherType.FORWARD));
	}


	private ClientIPSettingHandler applyClientIPDiscoveryHandler(Handler baseHandler, String endpointId)
	{
		ClientIPDiscovery ipDiscovery = new ClientIPDiscovery(serverSettings.getIntValue(PROXY_COUNT),
				serverSettings.getBooleanValue(ALLOW_NOT_PROXIED_TRAFFIC));
		IPValidator ipValidator = new IPValidator(
				serverSettings.getListOfValues(ALLOWED_IMMEDIATE_CLIENTS));

		log.info("Enabling client IP discovery filter");
		ClientIPSettingHandler handler = new ClientIPSettingHandler(ipDiscovery, ipValidator, endpointId);
		handler.setServer(theServer);
		handler.setHandler(baseHandler);
		return handler;
	}
}









