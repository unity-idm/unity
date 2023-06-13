/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.notifications.NotificationChannelInfo;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.NotificationsManagement;

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

	public CompatibleNotificationChannelsComboBox(EnumSet<CommunicationTechnology> supportedTechnologies,
			NotificationsManagement notChannelsMan)
	{
		this.notChannelsMan = notChannelsMan;
		reload(supportedTechnologies);
	}

	public void setDefaultValue()
	{
		if (values != null && !values.isEmpty())
			setValue(values.iterator().next());
		else
			setValue(null);
	}

	public void reload(EnumSet<CommunicationTechnology> supportedTechnologies)
	{
		if (supportedTechnologies.isEmpty())
		{
			values = Collections.emptyList();
			setItems(values);
			return;
		}

		Map<String, NotificationChannelInfo> channels = new HashMap<>();
		try
		{
			channels = notChannelsMan.getNotificationChannelsForTechnologies(supportedTechnologies);
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
