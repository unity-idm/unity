/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestURLQueryPrefillConfig;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;

public class URLQueryPrefillConfigMapperTest extends MapperTestBase<URLQueryPrefillConfig, RestURLQueryPrefillConfig>
{

	@Override
	protected URLQueryPrefillConfig getFullAPIObject()
	{
		return new URLQueryPrefillConfig("param", PrefilledEntryMode.DEFAULT);
	}

	@Override
	protected RestURLQueryPrefillConfig getFullRestObject()
	{
		return RestURLQueryPrefillConfig.builder()
				.withMode("DEFAULT")
				.withParamName("param")
				.build();
	}

	@Override
	protected Pair<Function<URLQueryPrefillConfig, RestURLQueryPrefillConfig>, Function<RestURLQueryPrefillConfig, URLQueryPrefillConfig>> getMapper()
	{
		return Pair.of(URLQueryPrefillConfigMapper::map, URLQueryPrefillConfigMapper::map);
	}

}
