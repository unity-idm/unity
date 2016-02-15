/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import java.util.Map;
import java.util.concurrent.Future;

import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.types.basic.EntityParam;

@Component
public class MockNotificationsProducer implements NotificationProducer
{
	private NotificationProducer mock;
	
	@Override
	public Future<NotificationStatus> sendNotification(EntityParam recipient,
			String channelName, String templateId, Map<String, String> params,
			String locale, String preferredAddress) throws EngineException
	{
		return mock.sendNotification(recipient, channelName, templateId, params, locale, preferredAddress);
	}

	@Override
	public Future<NotificationStatus> sendNotification(String recipientAddress,
			String channelName, String templateId, Map<String, String> params,
			String locale) throws EngineException
	{
		return mock.sendNotification(recipientAddress, channelName, templateId, params, locale);
	}

	@Override
	public void sendNotificationToGroup(String group, String channelName, String templateId,
			Map<String, String> params, String locale) throws EngineException
	{
		mock.sendNotificationToGroup(group, channelName, templateId, params, locale);
	}

	public NotificationProducer getAndResetMockFromMockito()
	{
		mock = Mockito.mock(NotificationProducer.class);
		return mock;
	}
}
