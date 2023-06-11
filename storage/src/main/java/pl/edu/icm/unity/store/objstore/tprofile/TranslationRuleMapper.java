/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.Optional;

import pl.edu.icm.unity.base.translation.TranslationRule;

public class TranslationRuleMapper
{
	public static DBTranslationRule map(TranslationRule translationRule)
	{
		return DBTranslationRule.builder()
				.withCondition(translationRule.getCondition())
				.withAction(Optional.ofNullable(translationRule.getAction())
						.map(TranslationActionMapper::map)
						.orElse(null))
				.build();
	}

	public static TranslationRule map(DBTranslationRule restTranslationRule)
	{
		return new TranslationRule(restTranslationRule.condition, Optional.ofNullable(restTranslationRule.action)
				.map(TranslationActionMapper::map)
				.orElse(null));
	}
}
