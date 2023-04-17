/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions;

import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TLSRetrievalProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, TLSRetrievalProperties.class);

	@DocumentationReferencePrefix
	public static final String P = "retrieval.tls.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults = new HashMap<>();

	public static final String NAME = "name";
	public static final String REGISTRATION_FORM_FOR_UNKNOWN = "registrationFormForUnknown";
	public static final String ENABLE_ASSOCIATION = "enableAssociation";
	
	
	static
	{
		defaults.put(NAME, new PropertyMD().setCanHaveSubkeys()
				.setDescription("Label to be used on UI for this option. "
						+ "Can have multiple language variants defined with subkeys."));

		defaults.put(REGISTRATION_FORM_FOR_UNKNOWN,
				new PropertyMD().setDescription(
						"Registration form " + "to be presented for unknown locally users who "
								+ "were correctly authenticated remotely."));
		defaults.put(ENABLE_ASSOCIATION,
				new PropertyMD("false").setDescription("Whether to present "
						+ "account association option for unknown locally users who "
						+ "were correctly authenticated remotely."));

		
		defaults.put("i18nName", new PropertyMD().setCanHaveSubkeys()
				.setDescription("Deprecated and ignored. Use name property instead!").setDeprecated());
		defaults.put("logoURL", new PropertyMD()
				.setDescription("URL of a logo to be used for this authN option on UI").setDeprecated());
	}
	
	public TLSRetrievalProperties(Properties properties)
	{
		super(P, properties, defaults, log);
	}
	
	Properties getProperties()
	{
		return properties;
	}
}
