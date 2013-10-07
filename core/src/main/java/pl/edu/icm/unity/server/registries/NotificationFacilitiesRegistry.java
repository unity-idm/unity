/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.notifications.NotificationFacility;

/**
 * Maintains a simple registry of available {@link NotificationFacility}ies.
 * @author K. Benedyczak
 */
@Component
public class NotificationFacilitiesRegistry extends TypesRegistryBase<NotificationFacility>
{
	@Autowired
	public NotificationFacilitiesRegistry(List<NotificationFacility> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(NotificationFacility from)
	{
		return from.getName();
	}
}
