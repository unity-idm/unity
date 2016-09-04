/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;

/**
 * Contains settings which are common for all IdP endpoints
 * @author Krzysztof Benedyczak
 */
public class CommonIdPProperties
{
	public static final String SKIP_CONSENT = "skipConsent";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	public static final String ASSUME_FORCE = "assumeForceOnSessionClash";
	public static final String SKIP_USERIMPORT = "skipUserImport";
	
	
	public static Map<String, PropertyMD> getDefaultsWithCategory(DocumentationCategory category,
			String defaultProfileMessage)
	{
		Map<String, PropertyMD> defaults = getDefaults(defaultProfileMessage);
		for (PropertyMD md: defaults.values())
			md.setCategory(category);
		return defaults;
	}
	
	public static Map<String, PropertyMD> getDefaults(String defaultProfileMessage)
	{
		Map<String, PropertyMD> defaults = new HashMap<String, PropertyMD>();
		defaults.put(SKIP_CONSENT, new PropertyMD("false").
				setDescription("Controls whether the user being authenticated should see the consent screen"
						+ " with the information what service requested authorization and what data "
						+ "is going to be released. Note that user may always choose to disable "
						+ "the consent screen for each service, even if this setting is set to false."));

		defaults.put(TRANSLATION_PROFILE, new PropertyMD().
				setDescription(defaultProfileMessage));

		defaults.put(ASSUME_FORCE, new PropertyMD("true").
				setDescription("Controls what to do in case of initialization of a new authentication, "
						+ "while another one was not finished within the same browser session."
						+ " By default a warning page is rendered and the user has "
						+ "a choice to cancel or forcefully continue. However, "
						+ "if this setting is set to true, then the new interaction forcefully"
						+ "takes over the old interaction, without asking the user."));
		defaults.put(SKIP_USERIMPORT, new PropertyMD("false").
				setDescription("Allows to completely disable user import functionality per endpoint."));
		return defaults;
	}

}
