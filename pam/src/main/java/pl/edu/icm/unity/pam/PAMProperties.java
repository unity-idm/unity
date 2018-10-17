/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;

public class PAMProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, PAMProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "pam.";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META=new HashMap<>();
	
	public static final String PAM_FACILITY = "facility";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	
	static 
	{
		META.put(PAM_FACILITY, new PropertyMD("unity").setDescription(
				"Name of PAM facility that should be used to authenticate users. "
				+ "Typically this is a filename in the pam.d directory."));
		META.put(TRANSLATION_PROFILE, new PropertyMD().setMandatory().setDescription("Name of a translation" +
				" profile, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));

	}
	
	public PAMProperties(Properties properties)
	{
		super(PREFIX, properties, META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
