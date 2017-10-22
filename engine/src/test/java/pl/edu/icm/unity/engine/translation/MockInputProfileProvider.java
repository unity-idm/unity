/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.SystemTranslationProfileProvider;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

@Component
public class MockInputProfileProvider implements SystemTranslationProfileProvider
{

	@Override
	public Map<String, TranslationProfile> getSystemProfiles()
	{
		Map<String, TranslationProfile> prof = new HashMap<String, TranslationProfile>();
		prof.put("demo", new TranslationProfile("demo", "demo", ProfileType.INPUT,
				ProfileMode.READ_ONLY, new ArrayList<>()));
		return prof;
	}

	@Override
	public ProfileType getSupportedType()
	{
		return ProfileType.INPUT;
	}

}
