/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBURLQueryPrefillConfigTest extends DBTypeTestBase<DBurlQueryPrefillConfig>
{

	@Override
	protected String getJson()
	{
		return "{\"paramName\":\"param\",\"mode\":\"DEFAULT\"}\n";
	}

	@Override
	protected DBurlQueryPrefillConfig getObject()
	{
		return DBurlQueryPrefillConfig.builder()
				.withMode("DEFAULT")
				.withParamName("param")
				.build();
	}

}
