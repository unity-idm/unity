/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.translation;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.translation.RestTranslationAction;
import io.imunity.rest.api.types.translation.RestTranslationProfile;
import io.imunity.rest.api.types.translation.RestTranslationProfile.Condition;
import io.imunity.rest.api.types.translation.RestTranslationProfile.RestTranslationProfileRule;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TranslationProfileMapperTest extends MapperTestBase<TranslationProfile, RestTranslationProfile>
{

	@Override
	protected TranslationProfile getFullAPIObject()
	{
		return new TranslationProfile("name", "desc", ProfileType.REGISTRATION, ProfileMode.DEFAULT,
				List.of(new TranslationRule("true", new TranslationAction("action", "p1", "p2"))));
	}

	@Override
	protected RestTranslationProfile getFullRestObject()
	{
		return RestTranslationProfile.builder()
				.withDescription("desc")
				.withName("name")
				.withMode("DEFAULT")
				.withType("REGISTRATION")
				.withRules(List.of(RestTranslationProfileRule.builder()
						.withCondition(Condition.builder()
								.withConditionValue("true")
								.build())
						.withAction(RestTranslationAction.builder()
								.withName("action")
								.withParameters(List.of("p1", "p2"))
								.build())
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<TranslationProfile, RestTranslationProfile>, Function<RestTranslationProfile, TranslationProfile>> getMapper()
	{
		return Pair.of(TranslationProfileMapper::map, TranslationProfileMapper::map);
	}

}
