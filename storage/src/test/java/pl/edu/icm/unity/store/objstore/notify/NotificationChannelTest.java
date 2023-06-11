/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.notify;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

public class NotificationChannelTest extends AbstractNamedWithTSTest<NotificationChannel>
{
	@Autowired
	private NotificationChannelDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<NotificationChannel> getDAO()
	{
		return dao;
	}

	@Override
	protected NotificationChannel getObject(String id)
	{
		NotificationChannel src = new NotificationChannel();
		src.setName(id);
		src.setConfiguration("configuration");
		src.setDescription("description");
		src.setFacilityId("facilityId");
		return src;
	}

	@Override
	protected NotificationChannel mutateObject(NotificationChannel src)
	{
		src.setName("name-Changed");
		src.setConfiguration("configuration2");
		src.setDescription("description2");
		src.setFacilityId("facilityId2");
		return src;
	}
}
