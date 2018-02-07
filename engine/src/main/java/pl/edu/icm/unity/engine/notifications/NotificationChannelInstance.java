/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.concurrent.Future;

import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;

/**
 * Configured object created by {@link NotificationFacility} able to send notifications.
 * 
 * @author K. Benedyczak
 */
public interface NotificationChannelInstance
{
	/**
	 * @return name of the facility this channel is associated with.
	 */
	public String getFacilityId();
	
	/**
	 * Sends a given message.
	 * @param recipientAddress resolved, facility specific address of a recipient
	 * @param message
	 * @param msgSubject message subject. Note that in case of some channels it may be ignored.
	 */
	public Future<NotificationStatus> sendNotification(String recipientAddress, Message message);
}
