/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.Map;
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
	 * Sends a message which is resolved from a given template with parameters.
	 * @param recipient
	 * @param channelName
	 * @param templateId
	 * @param params
	 * @param locale can be null. In such case the server's default locale will be used
	 * @return
	 * @throws EngineException
	 */
	public Future<NotificationStatus> sendNotification(EntityParam recipient, String channelName, 
			String templateId, Map<String, String> params, String locale) throws EngineException;

	/**
	 * Sends a message which is resolved from a given template with parameters.
	 * @param recipientAddress actual address of the recipient, as email address. 
	 * @param channelName
	 * @param templateId
	 * @param params
	 * @param locale can be null. In such case the server's default locale will be used
	 * @return
	 * @throws EngineException
	 */
	public Future<NotificationStatus> sendNotification(String recipientAddress, String channelName, 
			String templateId, Map<String, String> params, String locale) throws EngineException;
	
	/**
	 * Sends a message which is resolved from a given template with parameters.
	 * This version sends a message to all entities which are members of a given group and 
	 * have channel's address defined in this group. 
	 * @param group
	 * @param channelName
	 * @param templateId
	 * @param params
	 * @param locale can be null. In such case the server's default locale will be used
	 * @return
	 * @throws EngineException
	 */
	public void sendNotificationToGroup(String group, String channelName, 
			String templateId, Map<String, String> params, String locale) throws EngineException;
}
