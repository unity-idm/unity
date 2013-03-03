/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.server.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.unicore.security.canl.AuthnAndTrustProperties;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.FilePropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.jetty.HttpServerProperties;

/**
 * Principal options are defined here: ids and corresponding default values.
 * @author K. Benedyczak
 */
@Component
public class UnityServerConfiguration extends FilePropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, UnityServerConfiguration.class);
	public static final String CONFIGURATION_FILE = "conf/unityServer.conf";

	public static final String BASE_PREFIX = "unityServer.";
	
	@DocumentationReferencePrefix
	public static final String P = BASE_PREFIX + "core.";
	
	public static final String MAIL_CONF = "mailConfig";
	public static final String RECREATE_ENDPOINTS_ON_STARTUP = "recreateEndpointsOnStartup";
	public static final String ENDPOINTS = "endpoints.";
	public static final String ENDPOINT_DESCRIPTION = "description";
	public static final String ENDPOINT_TYPE = "type";
	public static final String ENDPOINT_CONFIGURATION = "configurationFile";
	public static final String ENDPOINT_ADDRESS = "contextPath";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	static
	{
		DocumentationCategory mainCat = new DocumentationCategory("General settings", "1");
		DocumentationCategory otherCat = new DocumentationCategory("Other", "8");
		
		defaults.put(MAIL_CONF, new PropertyMD("conf/mail.properties").setPath().setCategory(mainCat).
				setDescription("A configuration file for the mail notification subsystem."));
		defaults.put(RECREATE_ENDPOINTS_ON_STARTUP, new PropertyMD("false").setDescription(
				"If this options is true then all endpoints are initialized from configuration at each startup. If it is false then the persisted endpoints are loaded and configuration is used only at the initial start of the server."));
		
		defaults.put(ENDPOINTS, new PropertyMD().setStructuredList(true).setCategory(mainCat).
				setDescription("List of initially enabled endpoints"));
		defaults.put(ENDPOINT_TYPE, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(mainCat).
				setDescription("Endpoint type"));
		defaults.put(ENDPOINT_CONFIGURATION, new PropertyMD().setStructuredListEntry(ENDPOINTS).setPath().setCategory(mainCat).
				setDescription("Path of the file with JSON configuration of the endpoint"));
		defaults.put(ENDPOINT_DESCRIPTION, new PropertyMD("").setStructuredListEntry(ENDPOINTS).setCategory(mainCat).
				setDescription("Description of the endpoint"));
		defaults.put(ENDPOINT_ADDRESS, new PropertyMD().setStructuredListEntry(ENDPOINTS).setCategory(mainCat).
				setDescription("Context path of the endpoint"));
		
		defaults.put(TruststoreProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().setCategory(otherCat).
				setDescription("Properties starting with this prefix are used to configure server's trust settings and certificate validation. See separate documentation for details."));
		defaults.put(CredentialProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().setCategory(otherCat).
				setDescription("Properties starting with this prefix are used to configure server's credential. See separate documentation for details."));
		defaults.put(HttpServerProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().setCategory(otherCat).
				setDescription("Properties starting with this prefix are used to configure Jetty HTTP server settings. See separate documentation for details."));
		defaults.put(ClientProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().setCategory(otherCat).
				setDescription("Properties starting with this prefix are used to configure HTTP client settings, when UVOS server performs client calls (what happens very rarely e.g. when registering in registry). See separate documentation for details."));
	}

	private UnityHttpServerConfiguration jp;
	private IAuthnAndTrustConfiguration authnTrust;
	private IClientConfiguration clientCfg;
	
	@Autowired
	public UnityServerConfiguration(Environment env, ConfigurationLocationProvider locProvider) throws ConfigurationException, IOException
	{
		super(P, getConfigurationFile(env, locProvider), defaults, log);
		jp = new UnityHttpServerConfiguration(properties);
		authnTrust = new AuthnAndTrustProperties(properties, 
				P+TruststoreProperties.DEFAULT_PREFIX, P+CredentialProperties.DEFAULT_PREFIX);
		clientCfg = new ClientProperties(properties, P+ClientProperties.DEFAULT_PREFIX, authnTrust);
	}
	
	private static String getConfigurationFile(Environment env, ConfigurationLocationProvider locProvider)
	{
		String configFile;
		String[] nonOptionArgs = env.getProperty(CommandLinePropertySource.DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME, 
				String[].class);
		if (nonOptionArgs != null && nonOptionArgs.length > 0)
			configFile = nonOptionArgs[0];
		else 
			configFile = locProvider.getConfigurationLocation();
			
		log.debug("Using configuration file: " + configFile);
		return configFile;
	}
	
	public UnityHttpServerConfiguration getJettyProperties()
	{
		return jp;
	}
	
	public IAuthnAndTrustConfiguration getAuthAndTrust()
	{
		return authnTrust;
	}
	
	public IClientConfiguration getClientConfiguration()
	{
		return clientCfg;
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
