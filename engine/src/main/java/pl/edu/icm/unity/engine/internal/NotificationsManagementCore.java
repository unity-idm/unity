/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.NotificationsManagementImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.NotificationFacility;

/**
 * Code common to notifications management  {@link NotificationsManagementImpl} and {@link NotificationsProducer}. 
 * @author K. Benedyczak
 */
@Component
public class NotificationsManagementCore
{
	private Map<String, NotificationFacility> facilities;

	@Autowired
	public NotificationsManagementCore(Set<NotificationFacility> facilities)
	{
		this.facilities = new HashMap<>(facilities.size());
		for (NotificationFacility facility: facilities)
			this.facilities.put(facility.getName(), facility);
	}

	public Set<String> getNotificationFacilities() throws EngineException
	{
		return new HashSet<>(facilities.keySet());
	}

	public NotificationFacility getNotificationFacility(String name) throws EngineException
	{
		return facilities.get(name);
	}
}
