/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.server.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import eu.unicore.util.jetty.HttpServerProperties;

/**
 * Overrides defaults of {@link HttpServerProperties}, allowing anonymous SSL clients. 
 * This class also warns if typical misconfigurations are detected.
 * @author K. Benedyczak
 */
public class UnityHttpServerConfiguration extends HttpServerProperties
{	
	@DocumentationReferencePrefix
	public static final String PREFIX = UnityServerConfiguration.P+HttpServerProperties.DEFAULT_PREFIX;

	public static final String HTTPS_PORT = "port";
	public static final String HTTPS_HOST = "host";
	public static final String ADVERTISED_HOST = "advertisedHost";
	
	public static final String ENABLE_DOS_FILTER = "enableDoSFilter";
	public static final String DOS_FILTER_PFX = "dosFilter.";
	
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
				setDescription("The hostname or IP address (optionally with port), which is advertised externally whenever " +
					"the server has to provide its address. By default it is set to the listen address, " + 
					"however it must be set when the listen address is 0.0.0.0 and " +
					"also should be set whenever the server is listening on "
					+ "a private interface accessible via DNAT or similar solutions. Examples:"
					+ " +login.example.com+ or +login.example.com:8443+ "));		
		defaults.put(ENABLE_DOS_FILTER, new PropertyMD("false").
				setDescription("If enabled then the DenayOfService fileter is enabled for"
					+ "all services. The filter prevents DoS attacks, but requires "
					+ "a proper configuration dependent on the installation site."));
		defaults.put(DOS_FILTER_PFX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Under this prefix the settings of the DoS filter must be placed."
					+ " The reference of allowed settings is available in the Jetty "
					+ "DoS filter documentation, currently here: "
					+ "http://www.eclipse.org/jetty/documentation/current/dos-filter.html"));
		
		
		for (Map.Entry<String, PropertyMD> entry: HttpServerProperties.defaults.entrySet())
			defaults.put(entry.getKey(), entry.getValue().setCategory(advancedCat));
		defaults.put(ENABLE_GZIP, new PropertyMD("true").
				setDescription("Controls whether to enable compression of HTTP responses."));
		defaults.get(REQUIRE_CLIENT_AUTHN).setDefault("false");
	}

	public UnityHttpServerConfiguration(Properties source) throws ConfigurationException
	{
		super(source, PREFIX, defaults);
		String advertisedHost = getValue(ADVERTISED_HOST);
		if ("0.0.0.0".equals(getValue(HTTPS_HOST)) && advertisedHost == null)
			throw new ConfigurationException(getKeyDescription(ADVERTISED_HOST) + 
					" must be set when the listen address is 0.0.0.0 (all interfaces).");
		if (advertisedHost != null) {
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
	
	@Override
	public Set<String> getSortedStringKeys(String base, boolean allowListSubKeys)
	{
		return super.getSortedStringKeys(PREFIX+base, allowListSubKeys);
	}
	
	public String getProperty(String key)
	{
		return properties.getProperty(key);
	}
}
