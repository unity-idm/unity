package pl.edu.icm.unity.engine.mock;

import java.util.concurrent.Future;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.notifications.NotificationFacility;
import pl.edu.icm.unity.notifications.NotificationStatus;

public class MockNotificationFacility implements NotificationFacility
{

	@Override
	public String getName()
	{
		return "test";
	}

	@Override
	public String getDescription()
	{
		return "test";
	}

	@Override
	public void validateConfiguration(String configuration) throws WrongArgumentException
	{	
	}

	@Override
	public NotificationChannelInstance getChannel(String configuration)
	{
		return new NotificationChannelInstance()
		{
			
			@Override
			public Future<NotificationStatus> sendNotification(String recipientAddress,
					String msgSubject, String message)
			{
				
				return null;
			}
			
			@Override
			public String getFacilityId()
			{
				return "test";
			}
		};
	}

	@Override
	public String getRecipientAddressMetadataKey()
	{
		return "test";
	}

}
