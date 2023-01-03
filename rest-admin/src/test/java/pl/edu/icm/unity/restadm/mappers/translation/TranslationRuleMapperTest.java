/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.translation;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.translation.RestTranslationAction;
import io.imunity.rest.api.types.translation.RestTranslationRule;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TranslationRuleMapperTest extends MapperTestBase<TranslationRule, RestTranslationRule>
{

	@Override
	protected TranslationRule getAPIObject()
	{
		return new TranslationRule("true", new TranslationAction("action", "p1", "p2"));
	}

	@Override
	protected RestTranslationRule getRestObject()
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
