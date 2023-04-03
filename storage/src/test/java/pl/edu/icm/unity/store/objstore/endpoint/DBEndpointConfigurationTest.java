/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBEndpointConfigurationTest extends DBTypeTestBase<DBEndpointConfiguration>
{

	@Override
	protected String getJson()
	{
		return "{\"displayedName\":{\"DefaultValue\":\"disp\",\"Map\":{}},\"description\":\"desc\",\"configuration\":\"conf\","
				+ "\"realm\":\"realm\",\"tag\":\"tag1\",\"authenticationOptions\":[\"ao1\"]}\n";
	}

	@Override
	protected DBEndpointConfiguration getObject()
	{
		return DBEndpointConfiguration.builder()
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("disp")
						.build())
				.withDescription("desc")
				.withRealm("realm")
				.withTag("tag1")
				.withAuthenticationOptions(List.of("ao1"))
				.withConfiguration("conf")
				.build();
	}

}
