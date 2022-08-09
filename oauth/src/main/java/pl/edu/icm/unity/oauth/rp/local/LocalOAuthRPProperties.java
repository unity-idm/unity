/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.local;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

public class LocalOAuthRPProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, LocalOAuthRPProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.oauth2-local-rp.";
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();

	public static final String REQUIRED_SCOPES = "requiredScopes.";
	public static final String CREDENTIAL = "credential";

	static
	{
		META.put(REQUIRED_SCOPES,
				new PropertyMD().setList(false)
						.setDescription("Optional list of scopes which must be associated with the validated"
								+ " access token to make the authentication successful"));

		META.put(CREDENTIAL, new PropertyMD().setDescription("Password credential"));
	}

	public LocalOAuthRPProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

	public Properties getProperties()
	{
		return properties;
	}

}
