/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.types.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

public class URLQueryPrefillConfigMapper
{
	public static DBurlQueryPrefillConfig map(URLQueryPrefillConfig urlQueryPrefillConfig)
	{
		return DBurlQueryPrefillConfig.builder()
				.withMode(urlQueryPrefillConfig.mode.name())
				.withParamName(urlQueryPrefillConfig.paramName)
				.build();

	}

	public static URLQueryPrefillConfig map(DBurlQueryPrefillConfig restUrlQueryPrefillConfig)
	{
		return new URLQueryPrefillConfig(restUrlQueryPrefillConfig.paramName,
				PrefilledEntryMode.valueOf(restUrlQueryPrefillConfig.mode));
	}
}
