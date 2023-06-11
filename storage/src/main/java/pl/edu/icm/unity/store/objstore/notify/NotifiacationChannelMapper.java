/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.notify;

import pl.edu.icm.unity.base.notifications.NotificationChannel;

class NotifiacationChannelMapper
{
	static DBNotificationChannel map(NotificationChannel notificationChannel)
	{
		return DBNotificationChannel.builder()
				.withConfiguration(notificationChannel.getConfiguration())
				.withDescription(notificationChannel.getDescription())
				.withFacilityId(notificationChannel.getFacilityId())
				.withName(notificationChannel.getName())
				.build();
	}

	static NotificationChannel map(DBNotificationChannel dbNotificationChannel)
	{
		return new NotificationChannel(dbNotificationChannel.name, dbNotificationChannel.description,
				dbNotificationChannel.configuration, dbNotificationChannel.facilityId);
	}
}
