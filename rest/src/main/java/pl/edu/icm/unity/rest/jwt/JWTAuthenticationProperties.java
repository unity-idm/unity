/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt;

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
 * JWT endpoint's configuration.
 * @author K. Benedyczak
 */
public class JWTAuthenticationProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, JWTAuthenticationProperties.class);
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.jwtauthn.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();

	public static final String TOKEN_TTL = "tokenTtl";
	public static final String SIGNING_CREDENTIAL = "credential";
	
	static
	{
		META.put(TOKEN_TTL, new PropertyMD("3600").setPositive().setDescription(
				"Token validity time in seconds. Relevant only for token generation"));
		META.put(SIGNING_CREDENTIAL, new PropertyMD().setMandatory().setDescription("Name of the "
				+ "PKI credential that will be used to sign or verify tokens. "
				+ "Note that this must be an RSA credential."));
	}
	
	public JWTAuthenticationProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
