/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.translation;

import java.util.List;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestTranslationActionTest extends RestTypeBase<RestTranslationAction>
{

	@Override
	protected String getJson()
	{
		return "{\"name\":\"action\",\"parameters\":[\"p1\",\"p2\"]}";
	}

	@Override
	protected RestTranslationAction getObject()
	{
		return RestTranslationAction.builder()
				.withName("action")
				.withParameters(List.of(
				"p1", "p2" ))
				.build();
	}

}
