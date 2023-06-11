/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.Condition;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.DBTranslationProfileRule;

public class TranslationProfileMapper
{
	public static DBTranslationProfile map(TranslationProfile profile)
	{
		return DBTranslationProfile.builder()
				.withDescription(profile.getDescription())
				.withMode(profile.getProfileMode()
						.name())
				.withType(profile.getProfileType()
						.name())
				.withName(profile.getName())
				.withRules(Optional.ofNullable(profile.getRules())
						.map(r -> r.stream()
								.map(sr -> DBTranslationProfileRule.builder()
										.withAction(TranslationActionMapper.map(sr.getAction()))
										.withCondition(Condition.builder()
												.withConditionValue(sr.getCondition())
												.build())
										.build())
								.collect(Collectors.toList()))
						.orElse(null))
				.build();
	}

	public static TranslationProfile map(DBTranslationProfile restTranslationProfile)
	{
		return new TranslationProfile(restTranslationProfile.name, restTranslationProfile.description,
				ProfileType.valueOf(restTranslationProfile.type), getProfileMode(restTranslationProfile.mode),
				Optional.ofNullable(restTranslationProfile.rules)
						.map(r -> r.stream()
								.map(sr -> new TranslationRule(sr.condition.conditionValue,
										TranslationActionMapper.map(sr.action)))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()));

	}

	private static ProfileMode getProfileMode(String mode)
	{
		try
		{
			return ProfileMode.valueOf(mode);
		} catch (Exception e)
		{
			return ProfileMode.DEFAULT;
		}
	}

}
