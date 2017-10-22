/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation;

import java.util.Map;

import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Implementations (must be Spring components) provide additional system
 * translation profiles, defined for standard providers. Those profiles are not
 * saved in DB, but @{TranslationProfileManagement} also returns them. All
 * system profiles should be defined as read only profile
 * 
 * @author P.Piernik
 *
 */
public interface SystemTranslationProfileProvider
{
	/**
	 * 
	 * @return a list of translation profiles of this provider
	 */
	Map<String, TranslationProfile> getSystemProfiles();

	/**
	 * 
	 * @return a type of supported translation profile
	 */
	ProfileType getSupportedType();	
}
