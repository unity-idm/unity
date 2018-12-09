/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

public class PasswordRetrievalProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, PasswordRetrievalProperties.class);

	@DocumentationReferencePrefix
	public static final String P = "retrieval.password.";

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
		defaults.put(REGISTRATION_FORM_FOR_UNKNOWN, new PropertyMD()
				.setDescription("(Only used for remote password verification) Registration form "
						+ "to be presented for unknown locally users who "
						+ "were correctly authenticated remotely."));
		defaults.put(ENABLE_ASSOCIATION, new PropertyMD("false")
				.setDescription("(Only used for remote password verification) Whether to present "
						+ "account association option for unknown locally users who "
						+ "were correctly authenticated remotely."));
	}
	
	public PasswordRetrievalProperties(Properties properties)
	{
		super(P, properties, defaults, log);
	}
	
	Properties getProperties()
	{
		return properties;
	}
}
