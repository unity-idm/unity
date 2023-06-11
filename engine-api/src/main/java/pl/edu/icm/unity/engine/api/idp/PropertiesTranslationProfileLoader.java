/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;

/**
 * Retrieve translation profile from properties
 * 
 * @author P.Piernik
 *
 */
public class PropertiesTranslationProfileLoader
{
	public static TranslationProfile getTranslationProfile(UnityPropertiesHelper props, String globalProfileNameKey,
			String embeddedProfileKey) throws ConfigurationException
	{
		if (props.isSet(embeddedProfileKey))
		{
			return TranslationProfileGenerator.getProfileFromString(props.getValue(embeddedProfileKey));
		} else if (props.getValue(globalProfileNameKey) != null)
		{
			return TranslationProfileGenerator
					.generateIncludeOutputProfile(props.getValue(globalProfileNameKey));
		} else
		{
			return null;
		}
	}
}
