/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.notify;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.basic.NotificationChannel;

public class NotificationChannelMapperTest extends MapperTestBase<NotificationChannel, DBNotificationChannel>
{

	@Override
	protected NotificationChannel getFullAPIObject()
	{
		return new NotificationChannel("name", "desc", "config", "facility");
	}

	@Override
	protected DBNotificationChannel getFullDBObject()
	{
		return DBNotificationChannel.builder()
				.withName("name")
				.withDescription("desc")
				.withFacilityId("facility")
				.withConfiguration("config")
				.build();
	}

	@Override
	protected Pair<Function<NotificationChannel, DBNotificationChannel>, Function<DBNotificationChannel, NotificationChannel>> getMapper()
	{
		return Pair.of(NotifiacationChannelMapper::map, NotifiacationChannelMapper::map);
	}

}
