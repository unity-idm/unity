/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import java.util.Map;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBI18nStringTest extends DBTypeTestBase<DBI18nString>
{

	@Override
	protected String getJson()
	{
		return "{\"DefaultValue\":\"default\",\"Map\":{\"pl\":\"plval\",\"en\":\"enval\"}}";
	}

	@Override
	protected DBI18nString getObject()
	{
		return DBI18nString.builder()
				.withDefaultValue("default")
				.withValues(Map.of("pl", "plval", "en", "enval"))
				.build();
	}

}
