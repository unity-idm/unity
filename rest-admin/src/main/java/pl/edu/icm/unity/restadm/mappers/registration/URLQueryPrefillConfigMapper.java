/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import io.imunity.rest.api.types.registration.RestURLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;

public class URLQueryPrefillConfigMapper
{
	public static RestURLQueryPrefillConfig map(URLQueryPrefillConfig urlQueryPrefillConfig)
	{
		return RestURLQueryPrefillConfig.builder()
				.withMode(urlQueryPrefillConfig.mode.name())
				.withParamName(urlQueryPrefillConfig.paramName)
				.build();

	}

	public static URLQueryPrefillConfig map(RestURLQueryPrefillConfig restUrlQueryPrefillConfig)
	{
		return new URLQueryPrefillConfig(restUrlQueryPrefillConfig.paramName,
				PrefilledEntryMode.valueOf(restUrlQueryPrefillConfig.mode));
	}
}
