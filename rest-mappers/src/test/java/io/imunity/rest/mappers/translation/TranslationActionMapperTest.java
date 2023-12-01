/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.translation;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.translation.RestTranslationAction;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.types.translation.TranslationAction;

public class TranslationActionMapperTest extends MapperTestBase<TranslationAction, RestTranslationAction>
{

	@Override
	protected TranslationAction getFullAPIObject()
	{
		return new TranslationAction("action", "p1", "p2");
	}

	@Override
	protected RestTranslationAction getFullRestObject()
	{
		return RestTranslationAction.builder()
				.withName("action")
				.withParameters(List.of("p1", "p2"))
				.build();
	}

	@Override
	protected Pair<Function<TranslationAction, RestTranslationAction>, Function<RestTranslationAction, TranslationAction>> getMapper()
	{
		return Pair.of(TranslationActionMapper::map, TranslationActionMapper::map);
	}

}
