/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.translation;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.translation.RestTranslationAction;
import io.imunity.rest.api.types.translation.RestTranslationRule;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TranslationRuleMapperTest extends MapperTestBase<TranslationRule, RestTranslationRule>
{

	@Override
	protected TranslationRule getFullAPIObject()
	{
		return new TranslationRule("true", new TranslationAction("action", "p1", "p2"));
	}

	@Override
	protected RestTranslationRule getFullRestObject()
	{
		return RestTranslationRule.builder()
				.withCondition("true")
				.withAction(RestTranslationAction.builder()
						.withName("action")
						.withParameters(List.of("p1", "p2"))
						.build())
				.build();
	}

	@Override
	protected Pair<Function<TranslationRule, RestTranslationRule>, Function<RestTranslationRule, TranslationRule>> getMapper()
	{
		return Pair.of(TranslationRuleMapper::map, TranslationRuleMapper::map);
	}

}
