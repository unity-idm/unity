/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OTPRetrievalProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, OTPRetrievalProperties.class);

	@DocumentationReferencePrefix
	public static final String P = "otp.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults = new HashMap<>();

	public static final String NAME = "name";
	
	static
	{
		defaults.put(NAME, new PropertyMD().setCanHaveSubkeys()
				.setDescription("Label to be used on UI for this option. "
						+ "Can have multiple language variants defined with subkeys."));
	}
	
	public OTPRetrievalProperties(Properties properties)
	{
		super(P, properties, defaults, log);
	}
	
	Properties getProperties()
	{
		return properties;
	}
}
