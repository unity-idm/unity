/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.concurrent.Future;

import pl.edu.icm.unity.base.msg_template.MessageTemplate.Message;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;

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
	 * May be unavailable then {@link #providesMessageTemplatingFunctionality()} returns false
	 * @param recipientAddress resolved, facility specific address of a recipient
	 */
	public Future<NotificationStatus> sendNotification(String recipientAddress, Message message);

	/**
	 * Triggers sending a message from an external template with given parameters.
	 * Available only if {@link #providesMessageTemplatingFunctionality()} returns true
	 * @param recipientAddress resolved, facility specific address of a recipient
	 */
	public Future<NotificationStatus> sendExternalTemplateMessage(String recipientAddress, 
			MessageTemplateParams templateParams);
	
	/**
	 * @return true if the facility requires merely templateId and variables to send a message (i.e. has templating
	 * feature built in). If false is returned then internal Unity's templating functionality is used to produce
	 * an actual message to be sent.
	 */
	boolean providesMessageTemplatingFunctionality();
}
