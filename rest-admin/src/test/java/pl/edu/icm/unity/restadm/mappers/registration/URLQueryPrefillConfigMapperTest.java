/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestURLQueryPrefillConfig;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

public class URLQueryPrefillConfigMapperTest extends MapperTestBase<URLQueryPrefillConfig, RestURLQueryPrefillConfig>
{

	@Override
	protected URLQueryPrefillConfig getAPIObject()
	{
		return new URLQueryPrefillConfig("param", PrefilledEntryMode.DEFAULT);
	}

	@Override
	protected RestURLQueryPrefillConfig getRestObject()
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
