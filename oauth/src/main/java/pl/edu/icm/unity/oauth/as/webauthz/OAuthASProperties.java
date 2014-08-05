/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

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
 * Configuration of the OAuth AS. Used for OpenID Connect mode too. Shared by web and rest endpoints (authz and token
 * respectively).
 * @author K. Benedyczak
 */
public class OAuthASProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, OAuthASProperties.class);
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.as.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults = new HashMap<String, PropertyMD>();
	
	public static final String CLIENTS_GROUP = "clientsGroup";
	
	static
	{
		defaults.put(CLIENTS_GROUP, new PropertyMD("/oauth-clients").
				setDescription("Groups in which authorized OAuth Clients must be members. "
						+ "OAuth related attributes defined in this group are used"
						+ "to configure the client."));
	}
	
	public OAuthASProperties(Properties properties) throws ConfigurationException
	{
		super(P, properties, defaults, log);
	}
}
