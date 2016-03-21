package pl.edu.icm.unity.engine.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.UserRequestState;


public class MockNotificationFacility implements NotificationFacility
{
	public static final String NAME = "test";

	private List<Message> sent = new ArrayList<>();
	
	@Override
	public String getName()
	{
		return NAME;
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
				sent.add(new Message(recipientAddress, msgSubject, message));
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
	public String getAddressForEntity(EntityParam recipient, SqlSession sql, String address)
			throws EngineException
	{
		throw new IllegalIdentityValueException("no address");
	}

	@Override
	public String getAddressForUserRequest(UserRequestState<?> currentRequest, SqlSession sql) throws EngineException
	{
		return null;
	}

	public void resetSent()
	{
		this.sent.clear();
	}
	
	public List<Message> getSent()
	{
		return sent;
	}

	public static class Message
	{
		public final String address;
		public final String subject;
		public final String message;
		/**
		 * @param address
		 * @param subject
		 * @param message
		 */
		public Message(String address, String subject, String message)
		{
			super();
			this.address = address;
			this.subject = subject;
			this.message = message;
		}
	}
}
