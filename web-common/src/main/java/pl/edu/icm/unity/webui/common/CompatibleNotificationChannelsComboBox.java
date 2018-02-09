/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * A {@link ComboBox} showing only the notification channels which are
 * compatible with a given facilities.
 * 
 * @author P. Piernik
 */
public class CompatibleNotificationChannelsComboBox extends ComboBox<String>
{
	private Collection<String> values;
	private NotificationsManagement notChannelsMan;

	public CompatibleNotificationChannelsComboBox(Set<String> facilites,
			NotificationsManagement notChannelsMan)
	{
		this.notChannelsMan = notChannelsMan;
		reload(facilites);
	}

	public void setDefaultValue()
	{
		if (values != null && !values.isEmpty())
			setValue(values.iterator().next());
		else
			setValue(null);
	}

	public void reload(Set<String> facilites)
	{
		if (facilites.isEmpty())
		{
			values = Collections.emptyList();
			setItems(values);
			return;
		}

		Map<String, NotificationChannel> channels;
		try
		{
			channels = notChannelsMan.getNotificationChannels();

		} catch (EngineException e)
		{
			channels = new HashMap<>();
		}
		values = new HashSet<>();
		for (NotificationChannel channel : channels.values())
		{
			if (facilites.contains(channel.getFacilityId()))
			{
				values.add(channel.getName());

			}
		}

		setItems(values);
	}

	public Collection<String> getItems()
	{
		return values;
	}
}
