/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.translation;

import java.util.Optional;

import io.imunity.rest.api.types.translation.RestTranslationRule;
import pl.edu.icm.unity.base.translation.TranslationRule;

public class TranslationRuleMapper
{
	public static RestTranslationRule map(TranslationRule translationRule)
	{
		return RestTranslationRule.builder()
				.withCondition(translationRule.getCondition())
				.withAction(Optional.ofNullable(translationRule.getAction())
						.map(TranslationActionMapper::map)
						.orElse(null))
				.build();
	}

	public static TranslationRule map(RestTranslationRule restTranslationRule)
	{
		return new TranslationRule(restTranslationRule.condition, Optional.ofNullable(restTranslationRule.action)
				.map(TranslationActionMapper::map)
				.orElse(null));
	}
}
