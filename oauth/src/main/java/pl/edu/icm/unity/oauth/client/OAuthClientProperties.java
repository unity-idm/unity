/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of OAuth client.
 * @author K. Benedyczak
 */
public class OAuthClientProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthClientProperties.class);
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.client.";

	public static final String PROVIDER_LOCATION = "providerLocation";
	public static final String CLIENT_ID = "clientId";
	public static final String SCOPES = "scopes";
	public static final String PARAMS = "extraParams.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static 
	{
		META.put(PROVIDER_LOCATION, new PropertyMD().setDescription("Location (URL) of OAuth2 provider's "
				+ "authorization endpoint"));
		META.put(CLIENT_ID, new PropertyMD().setDescription("Client identifier, obtained during Unity's "
				+ "registration at the provider"));
		META.put(SCOPES, new PropertyMD().setDescription("Space separated list of authorization scopes to "
				+ "be requested"));
		META.put(PARAMS, new PropertyMD().setList(false).setDescription("List of additional parameters to be included in "
				+ "authorization request. Values should be in the 'name=value' format"));
	}
	
	public OAuthClientProperties(Properties properties) throws ConfigurationException
	{
		super(P, properties, META, log);
		//TODO - validation!!
	}

	public Properties getProperties()
	{
		return properties;
	}
}
