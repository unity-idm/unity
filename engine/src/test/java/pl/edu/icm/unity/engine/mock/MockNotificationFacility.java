package pl.edu.icm.unity.engine.mock;

import java.util.concurrent.Future;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

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
	public String getAddressForEntity(EntityParam recipient, SqlSession sql)
			throws EngineException
	{
		throw new IllegalIdentityValueException("no address");
	}

	@Override
	public String getAddressForRegistrationRequest(RegistrationRequestState currentRequest,
			SqlSession sql) throws EngineException
	{
		return null;
	}

}
