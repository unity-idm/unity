/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class URLQueryPrefillConfigMapperTest extends MapperTestBase<URLQueryPrefillConfig, DBurlQueryPrefillConfig>
{

	@Override
	protected URLQueryPrefillConfig getFullAPIObject()
	{
		return new URLQueryPrefillConfig("param", PrefilledEntryMode.DEFAULT);
	}

	@Override
	protected DBurlQueryPrefillConfig getFullDBObject()
	{
		return DBurlQueryPrefillConfig.builder()
				.withMode("DEFAULT")
				.withParamName("param")
				.build();
	}

	@Override
	protected Pair<Function<URLQueryPrefillConfig, DBurlQueryPrefillConfig>, Function<DBurlQueryPrefillConfig, URLQueryPrefillConfig>> getMapper()
	{
		return Pair.of(URLQueryPrefillConfigMapper::map, URLQueryPrefillConfigMapper::map);
	}

}
