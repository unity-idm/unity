/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.server.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
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
	public static final String ADVERTISED_HOST = "advertisedHost";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	static
	{
		DocumentationCategory mainCat = new DocumentationCategory("General settings", "1");
		DocumentationCategory advancedCat = new DocumentationCategory("Advanced settings", "9");
		defaults.put(HTTPS_HOST, new PropertyMD("localhost").setCategory(mainCat).
				setDescription("The hostname or IP address for HTTPS connections. Use 0.0.0.0 to listen on all interfaces."));
		defaults.put(HTTPS_PORT, new PropertyMD("2443").setBounds(0, 65535).setCategory(mainCat).
				setDescription("The HTTPS port to be used. If zero (0) is set then a random free port is used."));
		defaults.put(ADVERTISED_HOST, new PropertyMD().setCategory(mainCat).
				setDescription("The hostname or IP address which is advertised externally whenever " +
						"the server has to provide its address. By default it is set to the listen address, " + 
						"however it must be set when the listen address is 0.0.0.0 and " +
						"also should be set whenver the server is listening on private interface accessible via DNAT or similar solutions."));		

		for (Map.Entry<String, PropertyMD> entry: HttpServerProperties.defaults.entrySet())
			defaults.put(entry.getKey(), entry.getValue().setCategory(advancedCat));

		defaults.put(USE_NIO, new PropertyMD("true").setCategory(advancedCat).
				setDescription("Controls whether the NIO connector be used. NIO is best suited under high-load, " +
						"when lots of connections exist that are idle for long periods."));
		defaults.get(REQUIRE_CLIENT_AUTHN).setDefault("false");
	}

	public UnityHttpServerConfiguration(Properties source) throws ConfigurationException
	{
		super(source, PREFIX, defaults);
		if ("0.0.0.0".equals(getValue(HTTPS_HOST)) && getValue(ADVERTISED_HOST) == null)
			throw new ConfigurationException(getKeyDescription(ADVERTISED_HOST) + 
					" must be set when the listen address is 0.0.0.0 (all interfaces).");
	}
}
