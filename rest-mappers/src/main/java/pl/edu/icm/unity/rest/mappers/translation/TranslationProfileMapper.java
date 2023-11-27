/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.translation;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.translation.RestTranslationProfile;
import io.imunity.rest.api.types.translation.RestTranslationProfile.Condition;
import io.imunity.rest.api.types.translation.RestTranslationProfile.RestTranslationProfileRule;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TranslationProfileMapper
{
	public static RestTranslationProfile map(TranslationProfile profile)
	{
		return RestTranslationProfile.builder()
				.withDescription(profile.getDescription())
				.withMode(profile.getProfileMode()
						.name())
				.withType(profile.getProfileType()
						.name())
				.withName(profile.getName())
				.withRules(Optional.ofNullable(profile.getRules())
						.map(r -> r.stream()
								.map(sr -> RestTranslationProfileRule.builder()
										.withAction(TranslationActionMapper.map(sr.getAction()))
										.withCondition(Condition.builder()
												.withConditionValue(sr.getCondition())
												.build())
										.build())
								.collect(Collectors.toList()))
						.orElse(null))
				.build();
	}

	public static TranslationProfile map(RestTranslationProfile restTranslationProfile)
	{
		return new TranslationProfile(restTranslationProfile.name, restTranslationProfile.description,
				ProfileType.valueOf(restTranslationProfile.type), ProfileMode.valueOf(restTranslationProfile.mode),
				Optional.ofNullable(restTranslationProfile.rules)
						.map(r -> r.stream()
								.map(sr -> new TranslationRule(sr.condition.conditionValue,
										TranslationActionMapper.map(sr.action)))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()));

	}
}
