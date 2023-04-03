/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBRegistrationContextTest extends DBTypeTestBase<DBRegistrationContext>
{

	@Override
	protected String getJson()
	{
		return "{\"isOnIdpEndpoint\":true,\"triggeringMode\":\"manualAtLogin\"}\n";
	}

	@Override
	protected DBRegistrationContext getObject()
	{
		return DBRegistrationContext.builder()
				.withIsOnIdpEndpoint(true)
				.withTriggeringMode("manualAtLogin")
				.build();
	}

}
