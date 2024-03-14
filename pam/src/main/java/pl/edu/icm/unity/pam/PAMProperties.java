/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PAMProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, PAMProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "pam.";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META=new HashMap<>();
	
	public static final String PAM_FACILITY = "facility";
	
	static 
	{
		META.put(PAM_FACILITY, new PropertyMD("unity").setDescription(
				"Name of PAM facility that should be used to authenticate users. "
				+ "Typically this is a filename in the pam.d directory."));
		META.put(CommonWebAuthnProperties.TRANSLATION_PROFILE, new PropertyMD().setDescription("Name of a translation" +
				" profile, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));
		META.put(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE, new PropertyMD().setHidden().setDescription("Translation" +
				" profile as json string, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));
	}
	
	public PAMProperties(Properties properties)
	{
		super(PREFIX, properties, META, log);
		
		if (!isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE)
				&& !isSet(CommonWebAuthnProperties.TRANSLATION_PROFILE))
		{
			throw new ConfigurationException(getKeyDescription(CommonWebAuthnProperties.TRANSLATION_PROFILE)
					+ " is mandatory");

		}
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
