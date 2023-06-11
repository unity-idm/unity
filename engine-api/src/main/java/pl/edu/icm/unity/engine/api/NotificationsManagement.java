/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.base.notifications.NotificationChannelInfo;

/**
 * Management and usage of notifications subsystem (email, sms, ...)
 * @author K. Benedyczak
 */
public interface NotificationsManagement
{
	/**
	 * @return set with names of all available notification facilities (implementations). E.g. email 
	 * sender can be a facility.
	 */
	public Set<String> getNotificationFacilities() throws EngineException;
	
	/**
	 * Creates a new channel for a given facility. E.g. a new email facility configured to use a concrete 
	 * SMTP server.
	 * @param toAdd
	 */
	public void addNotificationChannel(NotificationChannel toAdd) throws EngineException;
	
	/**
	 * Removes a specified channel.
	 * @param channelName
	 * @throws EngineException
	 */
	public void removeNotificationChannel(String channelName) throws EngineException;
	
	/**
	 * Changes configuration of an existing notification channel.
	 * @param channelName
	 * @param newConfiguration
	 * @throws EngineException
	 */
	public void updateNotificationChannel(String channelName, String newConfiguration) throws EngineException;
	
	/**
	 * 
	 * @return map of available notification channels.
	 * @throws EngineException
	 */
	public Map<String, NotificationChannelInfo> getNotificationChannels() throws EngineException;
	
	/**
	 * @return get available notification channels which are using given communication technologies.
	 */
	public Map<String, NotificationChannelInfo> getNotificationChannelsForTechnologies(
			EnumSet<CommunicationTechnology> facilites) throws EngineException;
}
