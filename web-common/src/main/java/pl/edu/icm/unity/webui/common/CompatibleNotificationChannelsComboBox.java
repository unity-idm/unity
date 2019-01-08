/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.base.utils.Log;
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
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, CompatibleNotificationChannelsComboBox.class);
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

		Map<String, NotificationChannel> channels = new HashMap<>();
		try
		{
			channels = notChannelsMan.getNotificationChannelsForFacilities(facilites);

		} catch (EngineException e)
		{
			LOG.error("Cannot get notification channels", e);
		}
		values = channels.keySet();
		setItems(values);
	}

	public Collection<String> getItems()
	{
		return values;
	}
}
