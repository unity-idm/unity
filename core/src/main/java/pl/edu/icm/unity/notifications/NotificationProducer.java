/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.concurrent.Future;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Notification sending facility. Should be used internally, i.e. the users shouldn't have the possibility to 
 * invoke operations of this interface directly.
 * 
 * @author K. Benedyczak
 */
public interface NotificationProducer
{
	/**
	 * Sends notification to a specified channel. The engine is responsible for establishing notification 
	 * address (email/telephone number etc) for a given recipient.  
	 * 
	 * NOTE: this operation requires no authorization. Therefore it is not suitable for any kind 
	 * of direct exposure to end users.
	 * 
	 * @param recipient
	 * @param channelName
	 * @param message
	 * @param msgSubject message subject, which might be ignored by some of the notification facilities.
	 * @return future object allowing to check notification sending state
	 * @throws EngineException
	 */
	public Future<NotificationStatus> sendNotification(EntityParam recipient, String channelName, 
			String msgSubject, String message) throws EngineException;
}
