/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.jetty.HttpServerProperties;

/**
 * Overrides defaults of {@link HttpServerProperties} in case of allowing of anonymous SSL clients 
 * and NIO is enabled by default. This class also warns if typical misconfigurations are detected.
 * @author K. Benedyczak
 */
public class UnityHttpServerConfiguration extends HttpServerProperties
{	
	@DocumentationReferencePrefix
	public static final String PREFIX = UnityServerConfiguration.P+HttpServerProperties.DEFAULT_PREFIX;

	public static final String HTTPS_PORT = "port";
	public static final String HTTPS_HOST = "host";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	static
	{
		defaults.putAll(HttpServerProperties.defaults);
		defaults.put(USE_NIO, new PropertyMD("true").
				setDescription("Controls whether the NIO connector be used. NIO is best suited under high-load, " +
						"when lots of connections exist that are idle for long periods."));
		defaults.get(REQUIRE_CLIENT_AUTHN).setDefault("false");
		defaults.put(HTTPS_HOST, new PropertyMD("localhost").
				setDescription("The hostname or IP address for HTTPS connections."));
		defaults.put(HTTPS_PORT, new PropertyMD("2443").setBounds(1, 65535).
				setDescription("The HTTPS port to be used."));
	}

	public UnityHttpServerConfiguration(Properties source) throws ConfigurationException
	{
		super(source, PREFIX, defaults);
	}
}
