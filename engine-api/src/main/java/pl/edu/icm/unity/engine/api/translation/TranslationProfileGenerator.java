/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.translation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.translation.in.action.IncludeInputProfileActionFactory;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Helpers methods for generating translation profiles
 * @author P.Piernik
 *
 */
public class TranslationProfileGenerator
{
	public static final String EMBEDDED_PROFILE = "Embedded";
	
	
	
	public static TranslationProfile generateIncludeInputProfile(String profileToInclude)
	{

		return generateIncludeInputProfile(EMBEDDED_PROFILE, profileToInclude);
	}
	
	
	public static TranslationProfile generateIncludeInputProfile(String name, String profileToInclude)
	{

		TranslationRule rule = new TranslationRule("true",
				new TranslationAction(IncludeInputProfileActionFactory.NAME, profileToInclude));
		return new TranslationProfile(name, "", ProfileType.INPUT, Arrays.asList(rule));
	}
	
	public static TranslationProfile getProfileFromString(String profile)
	{
		try
		{
			JsonNode tp = Constants.MAPPER.readTree(profile);
			return new TranslationProfile((ObjectNode) tp);
		} catch (IOException e)
		{
			throw new InternalException(
					"Can't deserialize provider's translation profile from JSON",
					e);
		}
	}


	public static TranslationProfile generateEmptyInputProfile()
	{
		return new TranslationProfile(EMBEDDED_PROFILE, "", ProfileType.INPUT, new ArrayList<>());
	}
}
