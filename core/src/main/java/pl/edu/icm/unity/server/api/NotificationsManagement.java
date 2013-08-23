/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;

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
	 * @param facilityId
	 * @param channelName
	 * @param configuration
	 */
	public void addNotificationChannel(String facilityId, String channelName, String configuration) 
			throws EngineException;
	
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
	 * @return map of available notification channels. Values of the map are the configurations of the channels.
	 * @throws EngineException
	 */
	public Map<String, String> getNotificationChannels() throws EngineException;
}
