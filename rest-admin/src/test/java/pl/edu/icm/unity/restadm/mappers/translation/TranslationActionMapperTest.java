/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.translation;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.translation.RestTranslationAction;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
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
