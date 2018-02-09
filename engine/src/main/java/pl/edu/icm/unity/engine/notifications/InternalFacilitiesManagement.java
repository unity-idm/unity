/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Helper interface, probably to be removed after complete transactions rewrite to AOP. 
 * Allows for retrieving {@link NotificationFacility} for a given channel.
 * 
 * @author K. Benedyczak
 */
public interface InternalFacilitiesManagement
{
	NotificationFacility getNotificationFacilityForChannel(String channelName) 
			throws EngineException;
	
	NotificationFacility getNotificationFacilityForMessageTemplate(String templateId) 
			throws EngineException;
}
