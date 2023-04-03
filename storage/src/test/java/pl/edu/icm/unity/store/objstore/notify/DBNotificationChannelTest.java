/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.notify;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBNotificationChannelTest extends DBTypeTestBase<DBNotificationChannel>
{

	@Override
	protected String getJson()
	{
		return "{\"description\":\"desc\",\"name\":\"name\",\"configuration\":\"config\",\"facilityId\":\"facility\"}\n";
	}

	@Override
	protected DBNotificationChannel getObject()
	{
		return DBNotificationChannel.builder()
				.withName("name")
				.withDescription("desc")
				.withFacilityId("facility")
				.withConfiguration("config")
				.build();
	}

}
