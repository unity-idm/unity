/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.engine.api.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import eu.unicore.util.jetty.HttpServerProperties;

/**
 * Configuration of the Jetty server, baseline for all HTTP based endpoints
 */
public class UnityHttpServerConfiguration extends PropertiesHelper
{	
	private static final Logger log = Log.getLogger(Log.CONFIGURATION, HttpServerProperties.class);
	
	public enum XFrameOptions 
	{
		deny("DENY"), sameOrigin("SAMEORIGIN"), allowFrom("ALLOW-FROM"), allow("");
		
		private String httpValue;
		
		XFrameOptions(String httpValue)
		{
			this.httpValue = httpValue;
		}
		
		public String toHttp()
		{
			return httpValue;
		}
	};
	
	@DocumentationReferencePrefix
	public static final String PREFIX = UnityServerConfiguration.P+HttpServerProperties.DEFAULT_PREFIX;

	public static final String HTTP_PORT = "port";
	public static final String HTTP_HOST = "host";
	public static final String ADVERTISED_HOST = "advertisedHost";
	public static final String DISABLE_TLS = "disableTLS";
	
	public static final String ENABLE_DOS_FILTER = "enableDoSFilter";
	public static final String DOS_FILTER_PFX = "dosFilter.";
	
	
	public static final String FAST_RANDOM = "fastRandom";
	public static final String MIN_THREADS = "minThreads";
	public static final String MAX_THREADS = "maxThreads";
	public static final String MAX_CONNECTIONS = "maxConnections";
	public static final String WANT_CLIENT_AUTHN = "wantClientAuthn";
	public static final String REQUIRE_CLIENT_AUTHN = "requireClientAuthn";
	public static final String DISABLED_CIPHER_SUITES = "disabledCipherSuites";
	public static final String GZIP_PREFIX = "gzip.";
	public static final String MIN_GZIP_SIZE = GZIP_PREFIX + "minGzipSize";
	public static final String ENABLE_GZIP = GZIP_PREFIX + "enable";
	public static final String ENABLE_HSTS = "enableHsts";
	public static final String FRAME_OPTIONS = "xFrameOptions";
	public static final String ALLOWED_TO_EMBED = "xFrameAllowed";
	public static final String MAX_IDLE_TIME = "maxIdleTime";
	
	/**
	 * CORS support. For the parameters see 
	 * https://www.eclipse.org/jetty/documentation/9.4.x/cross-origin-filter.html
	 */
	public static final String ENABLE_CORS = "enableCORS";
	public static final String CORS_PFX = "cors.";
	public static final String CORS_ALLOWED_ORIGINS = "allowedOrigins";
	public static final String CORS_ALLOWED_METHODS = "allowedMethods";
	public static final String CORS_ALLOWED_HEADERS = "allowedHeaders";
	public static final String CORS_ALLOW_CREDENTIALS = "allowCredentials";
	public static final String CORS_EXPOSED_HEADERS = "exposedHeaders";
	public static final String CORS_PREFLIGHT_MAX_AGE = "preflightMaxAge";
	public static final String CORS_CHAIN_PREFLIGHT = "chainPreflight";
	public static final String PROXY_COUNT = "proxyCount";
	public static final String ALLOWED_IMMEDIATE_CLIENTS = "allowedClientIPs.";
	public static final String ALLOW_NOT_PROXIED_TRAFFIC = "allowNotProxiedTraffic";

	private static final String SO_LINGER_TIME = "soLingerTime";
	private static final String HIGH_LOAD_CONNECTIONS = "highLoadConnections";
	private static final String LOW_RESOURCE_MAX_IDLE_TIME = "lowResourceMaxIdleTime";

	
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	static
	{
		DocumentationCategory mainCat = new DocumentationCategory("General settings", "1");
		DocumentationCategory corsCat = new DocumentationCategory("CORS settings", "7");
		DocumentationCategory proxyCat = new DocumentationCategory("Proxy settings", "8");
		DocumentationCategory advancedCat = new DocumentationCategory("Advanced settings", "9");
		defaults.put(HTTP_HOST, new PropertyMD("localhost").setCategory(mainCat).
				setDescription("The hostname or IP address for HTTP connections. Use 0.0.0.0 to listen on all interfaces."));
		defaults.put(HTTP_PORT, new PropertyMD("2443").setBounds(0, 65535).setCategory(mainCat).
				setDescription("The HTTP port to be used. If zero (0) is set then a random free port is used."));
		defaults.put(ADVERTISED_HOST, new PropertyMD().setCategory(mainCat).
				setDescription("The hostname or IP address (optionally with port), which is advertised externally whenever " +
					"the server has to provide its address. By default it is set to the listen address, " + 
					"however it must be set when the listen address is 0.0.0.0 and " +
					"also should be set whenever the server is listening on "
					+ "a private interface accessible via DNAT or similar solutions. Examples:"
					+ " +login.example.com+ or +login.example.com:8443+ "));		
		defaults.put(DISABLE_TLS, new PropertyMD("false").setCategory(mainCat).
				setDescription("If set to true then server will listen on plain, insecure socket. "
						+ "Useful when Unity is hidden behind a proxy server, "
						+ "which provides TLS on its own. "
						+ "Note: it is still mandatory for web browser clients to access Unity over HTTPS "
						+ "as otherwise Unity cookies won't be accepted by the browser. "
						+ "Therefore Unity's advertised address is always be HTTPS."));
		
		defaults.put(PROXY_COUNT, new PropertyMD("0").setMin(0).setMax(32).setCategory(proxyCat).
				setDescription("If set to 0 then it is assumed then this server is not behind a proxy. "
						+ "Otherwise the number should specify the number of (local, trusted) proxies "
						+ "that are protecting the server from the actual clients. "
						+ "In effect the assumed client IP will be taken from the X-Forwarded-For "
						+ "header, stripping the trailing ones from intermediary proxies. "
						+ "Not that only proxy servers setting X-Forwarded-For are supported."));
		defaults.put(ALLOWED_IMMEDIATE_CLIENTS, new PropertyMD().setList(false).setCategory(proxyCat).
				setDescription("If not empty then contains a list of IPv4 or IPv6 addresses,"
						+ "that are allowed as immediate clients. In practice it is useful"
						+ " when Unity is deployed behind a proxy server: "
						+ "then proxy IP(s) should be entered as the only allowed IP to harden installation security. "
						+ "Note: CIDR notation can be used to denote networks, e.g. 10.10.0.0/16."));
		defaults.put(ALLOW_NOT_PROXIED_TRAFFIC, new PropertyMD("true").setCategory(proxyCat).
				setDescription("If false then only requests with X-Forwarded-For header will be accepted."));
		
		
		defaults.put(ENABLE_DOS_FILTER, new PropertyMD("false").setCategory(advancedCat).
				setDescription("If enabled then the DenayOfService fileter is enabled for"
					+ "all services. The filter prevents DoS attacks, but requires "
					+ "a proper configuration dependent on the installation site."));
		defaults.put(DOS_FILTER_PFX, new PropertyMD().setCanHaveSubkeys().setCategory(advancedCat).
				setDescription("Under this prefix the settings of the DoS filter must be placed."
					+ " The reference of allowed settings is available in the Jetty "
					+ "DoS filter documentation, currently here: "
					+ "http://www.eclipse.org/jetty/documentation/current/dos-filter.html"));
		
		defaults.put(MAX_THREADS, new PropertyMD("255").setCategory(advancedCat).
				setDescription("Maximum number of threads to have in the thread pool for processing HTTP connections."
						+ " Note that this number will be increased with few additional "
						+ "threads to handle connectors."));
		defaults.put(MIN_THREADS, new PropertyMD("1").setPositive().setCategory(advancedCat).
				setDescription("Minimum number of threads to have in the thread pool for processing HTTP connections. "
						+ " Note that this number will be increased with few additional "
						+ "threads to handle connectors."));
		defaults.put(MAX_CONNECTIONS, new PropertyMD("256").setCategory(advancedCat).
				setDescription("Maximum number of concurrent connections the server accepts."));
		defaults.put(FAST_RANDOM, new PropertyMD("false").setCategory(advancedCat).
				setDescription("Use insecure, but fast pseudo random generator to generate session ids "
						+ "instead of secure generator for SSL sockets."));
		defaults.put(WANT_CLIENT_AUTHN, new PropertyMD("true").setCategory(advancedCat).
				setDescription("Controls whether the SSL socket accepts (but does not require) client-side authentication."));
		defaults.put(DISABLED_CIPHER_SUITES, new PropertyMD("").setCategory(advancedCat).
				setDescription("Space separated list of SSL cipher suites to be disabled. "
						+ "Names of the ciphers must adhere to the standard Java cipher names, available here: "
						+ "http://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SupportedCipherSuites"));
		defaults.put(MIN_GZIP_SIZE, new PropertyMD("100000").setCategory(advancedCat).
				setDescription("Specifies the minimal size of message that should be compressed."));
		
		defaults.put(ENABLE_HSTS, new PropertyMD("false").setCategory(advancedCat).
				setDescription("Control whether HTTP strict transport security is enabled. "
						+ "It is a good and strongly suggested security mechanism for all production sites. "
						+ "At the same time it can not be used with self-signed or not "
						+ "issued by a generally trusted CA server certificates, "
						+ "as with HSTS a user can't opt in to enter such site."));
		defaults.put(FRAME_OPTIONS, new PropertyMD(XFrameOptions.deny).setCategory(advancedCat).
				setDescription("Defines whether a clickjacking prevention should be turned on, by insertion"
						+ "of the X-Frame-Options HTTP header. The 'allow' value disables the feature."
						+ " See the RFC 7034 for details. Note that for the 'allowFrom' "
						+ "you should define also the " + ALLOWED_TO_EMBED + 
						" option and it is not fully supported by all the browsers."));
		defaults.put(ALLOWED_TO_EMBED, new PropertyMD("http://localhost").setCategory(advancedCat).
				setDescription("URI origin that is allowed to embed web interface inside a (i)frame."
						+ " Meaningful only if the " + FRAME_OPTIONS + " is set to 'allowFrom'."
						+ " The value should be in the form: 'http[s]://host[:port]'"));
		defaults.put(ENABLE_GZIP, new PropertyMD("true").setCategory(advancedCat).
				setDescription("Controls whether to enable compression of HTTP responses."));
		defaults.put(REQUIRE_CLIENT_AUTHN, new PropertyMD("false").setCategory(advancedCat).
				setDescription("Controls whether the SSL socket requires client-side authentication."));
		defaults.put(MAX_IDLE_TIME, new PropertyMD("200000").setPositive().
				setDescription("Time (in ms.) before an idle connection will time out. It should be "
						+ "large enough not to expire connections with slow clients, "
						+ "values below 30s are getting quite risky."));
		
		defaults.put(ENABLE_CORS, new PropertyMD("false").setCategory(corsCat).
				setDescription("Control whether Cross-Origin Resource Sharing is enabled. "
						+ "Enable to allow e.g. accesing REST services from client-side JavaScript."));
		defaults.put(CORS_PFX, new PropertyMD().setCanHaveSubkeys().setCategory(corsCat).
				setDescription("Common prefix under which CORS is configured"));
		defaults.put(CORS_PFX + CORS_ALLOWED_ORIGINS, new PropertyMD("*").setCategory(corsCat).
				setDescription("Allowed script origins."));
		defaults.put(CORS_PFX + CORS_ALLOWED_METHODS, new PropertyMD("GET,PUT,POST,DELETE,HEAD").setCategory(corsCat).
				setDescription("Comma separated list of allowed HTTP verbs."));
		defaults.put(CORS_PFX + CORS_ALLOWED_HEADERS, new PropertyMD("*").setCategory(corsCat).
				setDescription("Comma separated list of allowed HTTP headers (default: any)"));
		defaults.put(CORS_PFX + CORS_ALLOW_CREDENTIALS, new PropertyMD("true").setCategory(corsCat).
				setDescription("Whether the server allows requests with credentials"));
		defaults.put(CORS_PFX + CORS_PREFLIGHT_MAX_AGE, new PropertyMD("1800").setCategory(corsCat).
				setDescription("The number of seconds that preflight requests can be cached by the client."));
		defaults.put(CORS_PFX + CORS_EXPOSED_HEADERS, new PropertyMD("Location,Content-Type").setCategory(corsCat).
				setDescription("Comma separated list of HTTP headers that are allowed to be exposed to the client."));
		defaults.put(CORS_PFX + CORS_CHAIN_PREFLIGHT, new PropertyMD("false").setCategory(corsCat).
				setDescription("Whether preflight OPTION requests are chained (passed on) "
						+ "to the resource or handled via the CORS filter."));
		
		
		defaults.put(SO_LINGER_TIME, new PropertyMD().setDeprecated().
				setDescription("Not used anymore with non-blocking server connectors. Please remove from configuration."));
		defaults.put(HIGH_LOAD_CONNECTIONS, new PropertyMD().setDeprecated().
				setDescription("Deprecated and ignored. Use maxConnections option instead."));
		defaults.put(LOW_RESOURCE_MAX_IDLE_TIME, new PropertyMD().setDeprecated().
				setDescription("Not used anymore without a counterpart"));
	}

	public UnityHttpServerConfiguration(Properties source) throws ConfigurationException
	{
		super(PREFIX, source, defaults, log);
		String advertisedHost = getValue(ADVERTISED_HOST);
		if ("0.0.0.0".equals(getValue(HTTP_HOST)) && advertisedHost == null)
			throw new ConfigurationException(getKeyDescription(ADVERTISED_HOST) + 
					" must be set when the listen address is 0.0.0.0 (all interfaces).");
		if (advertisedHost != null) 
		{
			if (advertisedHost.contains("://"))
				throw new ConfigurationException(getKeyDescription(ADVERTISED_HOST) + 
						" must contain hostname and optionally the port, "
						+ "but not the protocol prefix.");
			try
			{
				new URL("https://" + advertisedHost);
			} catch (MalformedURLException e)
			{
				throw new ConfigurationException(getKeyDescription(ADVERTISED_HOST) + 
						" is invalid, URL can not be constructed from it", e);
			}
		}
	}
	
	public Set<String> getSortedStringKeys(String configKey)
	{
		return getSortedStringKeys(prefix+configKey, false);
	}
	
	public String getProperty(String key)
	{
		return properties.getProperty(key);
	}
}
