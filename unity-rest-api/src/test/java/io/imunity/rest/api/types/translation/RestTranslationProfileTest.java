/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.translation;

import java.util.List;

import io.imunity.rest.api.types.basic.RestTypeBase;
import io.imunity.rest.api.types.translation.RestTranslationProfile.Condition;
import io.imunity.rest.api.types.translation.RestTranslationProfile.RestTranslationProfileRule;

public class RestTranslationProfileTest extends RestTypeBase<RestTranslationProfile>
{

	@Override
	protected String getJson()
	{
		return "{\"ver\":\"2\",\"name\":\"name\",\"description\":\"desc\",\"type\":\"REGISTRATION\",\"mode\":\"DEFAULT\","
				+ "\"rules\":[{\"condition\":{\"conditionValue\":\"true\"},\"action\":{\"name\":\"action\","
				+ "\"parameters\":[\"p1\",\"p2\"]}}]}\n";
	}

	@Override
	protected RestTranslationProfile getObject()
	{
		return RestTranslationProfile.builder()
				.withDescription("desc")
				.withName("name")
				.withMode("DEFAULT")
				.withType("REGISTRATION")
				.withRules(List.of(RestTranslationProfileRule.builder()
						.withCondition(Condition.builder().withConditionValue("true").build())
						.withAction(RestTranslationAction.builder()
								.withName("action")
								.withParameters(List.of("p1", "p2"))
								.build())
						.build()))
				.build();
	}

}
