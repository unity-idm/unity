/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.server.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
	
	public static final String ENABLED_LOCALES = "enabledLocales.";
	public static final String DEFAULT_LOCALE = "defaultLocale";
	public static final String MAIL_CONF = "mailConfig";
	public static final String THREAD_POOL_SIZE = "threadPoolSize";
	public static final String RECREATE_ENDPOINTS_ON_STARTUP = "recreateEndpointsOnStartup";
	public static final String ENDPOINTS = "endpoints.";
	public static final String ENDPOINT_DESCRIPTION = "endpointDescription";
	public static final String ENDPOINT_TYPE = "endpointType";
	public static final String ENDPOINT_CONFIGURATION = "endpointConfigurationFile";
	public static final String ENDPOINT_ADDRESS = "contextPath";
	public static final String ENDPOINT_NAME = "endpointName";	
	public static final String ENDPOINT_AUTHENTICATORS = "endpointAuthenticators";
	public static final String INITIALIZERS = "initializers.";

	public static final String AUTHENTICATORS = "authenticators.";
	public static final String AUTHENTICATOR_NAME = "authenticatorName";
	public static final String AUTHENTICATOR_TYPE = "authenticatorType";
	public static final String AUTHENTICATOR_CREDENTIAL = "localCredential";
	public static final String AUTHENTICATOR_VERIFICATOR_CONFIG = "verificatorConfigurationFile";
	public static final String AUTHENTICATOR_RETRIEVAL_CONFIG = "retrievalConfigurationFile";
	
	public static final String CREDENTIALS = "credentials.";
	public static final String CREDENTIAL_NAME = "credentialName";
	public static final String CREDENTIAL_TYPE = "credentialType";
	public static final String CREDENTIAL_DESCRIPTION = "credentialDescription";
	public static final String CREDENTIAL_CONFIGURATION = "credentialConfigurationFile";

	public static final String CREDENTIAL_REQS = "credentialRequirements.";
	public static final String CREDENTIAL_REQ_NAME = "credentialReqName";
	public static final String CREDENTIAL_REQ_DESCRIPTION = "credentialReqDescription";
	public static final String CREDENTIAL_REQ_CONTENTS = "credentialReqContents.";
	
	public static final String INITIAL_ADMIN_USER = "initialAdminUsername";
	public static final String INITIAL_ADMIN_PASSWORD = "initialAdminPassword";
	

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	static
	{
		DocumentationCategory mainCat = new DocumentationCategory("General settings", "1");
		DocumentationCategory otherCat = new DocumentationCategory("Other", "8");
		
		defaults.put(ENABLED_LOCALES, new PropertyMD().setList(true).setCategory(mainCat).
				setDescription("List of enabled locales. " +
				"Each entry must have a language code as 'en' or 'pl' first, " +
				"and then, after a space an optional, short name which will be presented in the UI. By default the 'en' locale is installed."));
		defaults.put(DEFAULT_LOCALE, new PropertyMD("en").setCategory(mainCat).
				setDescription("The default locale to be used. Must be one of the enabled locales."));
		defaults.put(MAIL_CONF, new PropertyMD("conf/mail.properties").setPath().setCategory(mainCat).
				setDescription("A configuration file for the mail notification subsystem."));
		defaults.put(RECREATE_ENDPOINTS_ON_STARTUP, new PropertyMD("false").setDescription(
				"If this options is true then all endpoints are initialized from configuration at each startup. If it is false then the persisted endpoints are loaded and configuration is used only at the initial start of the server."));
		defaults.put(THREAD_POOL_SIZE, new PropertyMD("4").setDescription(
				"Number of threads used by internal processes of the server. HTTP server threads use a separate pool."));
		defaults.put(INITIALIZERS, new PropertyMD().setList(true).setDescription(
				"List of identifiers of initialization modules that should be run on the first startup."));
		
		defaults.put(ENDPOINTS, new PropertyMD().setStructuredList(true).setCategory(mainCat).
				setDescription("List of initially enabled endpoints"));
		defaults.put(ENDPOINT_TYPE, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(mainCat).
				setDescription("Endpoint type"));
		defaults.put(ENDPOINT_CONFIGURATION, new PropertyMD().setStructuredListEntry(ENDPOINTS).setPath().setMandatory().setCategory(mainCat).
				setDescription("Path of the file with JSON configuration of the endpoint"));
		defaults.put(ENDPOINT_DESCRIPTION, new PropertyMD("").setStructuredListEntry(ENDPOINTS).setCategory(mainCat).
				setDescription("Description of the endpoint"));
		defaults.put(ENDPOINT_ADDRESS, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(mainCat).
				setDescription("Context path of the endpoint"));
		defaults.put(ENDPOINT_NAME, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(mainCat).
				setDescription("Endpoint name"));
		defaults.put(ENDPOINT_AUTHENTICATORS, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(mainCat).
				setDescription("Endpoint authenticator names: each set is separated with ';' and particular authenticators in each set with ','."));

		defaults.put(AUTHENTICATORS, new PropertyMD().setStructuredList(true).setCategory(mainCat).
				setDescription("List of initially enabled authenticators"));
		defaults.put(AUTHENTICATOR_NAME, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setMandatory().setCategory(mainCat).
				setDescription("Authenticator name"));
		defaults.put(AUTHENTICATOR_TYPE, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setMandatory().setCategory(mainCat).
				setDescription("Authenticator type"));
		defaults.put(AUTHENTICATOR_CREDENTIAL, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setCategory(mainCat).
				setDescription("For local authenticator the name of the local credential associated with it."));
		defaults.put(AUTHENTICATOR_VERIFICATOR_CONFIG, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setCategory(mainCat).
				setDescription("Authenticator configuration file of the verificator"));
		defaults.put(AUTHENTICATOR_RETRIEVAL_CONFIG, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setMandatory().setCategory(mainCat).
				setDescription("Authenticator configuration file of the retrieval"));

		defaults.put(CREDENTIALS, new PropertyMD().setStructuredList(true).setCategory(mainCat).
				setDescription("List of initially defined credentials"));
		defaults.put(CREDENTIAL_NAME, new PropertyMD().setStructuredListEntry(CREDENTIALS).setMandatory().setCategory(mainCat).
				setDescription("Credential name"));
		defaults.put(CREDENTIAL_TYPE, new PropertyMD().setStructuredListEntry(CREDENTIALS).setMandatory().setCategory(mainCat).
				setDescription("Credential type"));
		defaults.put(CREDENTIAL_DESCRIPTION, new PropertyMD("").setStructuredListEntry(CREDENTIALS).setCategory(mainCat).
				setDescription("Credential description"));
		defaults.put(CREDENTIAL_CONFIGURATION, new PropertyMD().setStructuredListEntry(CREDENTIALS).setMandatory().setCategory(mainCat).
				setDescription("Credential configuration file"));

		defaults.put(CREDENTIAL_REQS, new PropertyMD().setStructuredList(true).setCategory(mainCat).
				setDescription("List of initially defined credential requirements"));
		defaults.put(CREDENTIAL_REQ_NAME, new PropertyMD().setStructuredListEntry(CREDENTIAL_REQS).setMandatory().setCategory(mainCat).
				setDescription("Credential requirement name"));
		defaults.put(CREDENTIAL_REQ_DESCRIPTION, new PropertyMD("").setStructuredListEntry(CREDENTIAL_REQS).setCategory(mainCat).
				setDescription("Credential requirement description"));
		defaults.put(CREDENTIAL_REQ_CONTENTS, new PropertyMD().setStructuredListEntry(CREDENTIAL_REQS).setList(false).setMandatory().setCategory(mainCat).
				setDescription("Credential requirement contents, i.e. credentials that belongs to it"));

		defaults.put(INITIAL_ADMIN_USER, new PropertyMD("admin").setCategory(mainCat).
				setDescription("Username of the administrator to be installed to the empty database."));
		defaults.put(INITIAL_ADMIN_PASSWORD, new PropertyMD("admin").setCategory(mainCat).
				setDescription("Password of the administrator to be installed to the empty database."));
		
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
	private Map<String, Locale> enabledLocales;
	private Locale defaultLocale;
	
	@Autowired
	public UnityServerConfiguration(Environment env, ConfigurationLocationProvider locProvider) throws ConfigurationException, IOException
	{
		super(P, getConfigurationFile(env, locProvider), defaults, log);
		jp = new UnityHttpServerConfiguration(properties);
		authnTrust = new AuthnAndTrustProperties(properties, 
				P+TruststoreProperties.DEFAULT_PREFIX, P+CredentialProperties.DEFAULT_PREFIX);
		clientCfg = new ClientProperties(properties, P+ClientProperties.DEFAULT_PREFIX, authnTrust);
		enabledLocales = loadEnabledLocales();
		defaultLocale = safeLocaleDecode(getValue(DEFAULT_LOCALE));
		if (!isLocaleSupported(defaultLocale))
			throw new ConfigurationException("The default locale is not among enabled ones.");
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
	
	/**
	 * @return map with enabled locales. Key is the user-friendly label. 
	 */
	private Map<String, Locale> loadEnabledLocales()
	{
		List<String> locales = getListOfValues(ENABLED_LOCALES);
		if (locales.isEmpty())
		{
			locales = new ArrayList<String>();
			locales.add("en English");
		}
		Map<String, Locale> ret = new LinkedHashMap<String, Locale>();
		for (String locale: locales)
		{
			locale = locale.trim() + " ";
			int split = locale.indexOf(' ');
			String code = locale.substring(0, split);
			String name = locale.substring(split).trim();
			if (name.equals(""))
				name = code;
			Locale l = safeLocaleDecode(code);
			ret.put(name, l);
		}
		return ret;
	}
	
	public boolean isLocaleSupported(Locale toSearch)
	{
		for (Locale l: enabledLocales.values())
			if (l.equals(toSearch))
				return true;
		return false;
	}
	
	public static Locale safeLocaleDecode(String inputRaw)
	{
		if (inputRaw == null)
			return Locale.ENGLISH;
		Locale l;
		String input = inputRaw.trim();
		if (input.contains("_"))
		{
			String[] sp = input.split("_");
			l = new Locale(sp[0], sp[1]);
		} else
		{
			l = new Locale(input);
		}
		return l;
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
	
	public Locale getDefaultLocale()
	{
		return defaultLocale;
	}

	public Map<String, Locale> getEnabledLocales()
	{
		return enabledLocales;
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
