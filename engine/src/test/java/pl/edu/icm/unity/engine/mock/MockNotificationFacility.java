package pl.edu.icm.unity.engine.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.notifications.MessageTemplateParams;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;

@Component
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
	public CommunicationTechnology getTechnology()
	{
		return CommunicationTechnology.EMAIL;
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
					MessageTemplate.Message message)
			{
				sent.add(new Message(recipientAddress, message.getSubject(), message.getBody()));
				return null;
			}
			
			@Override
			public String getFacilityId()
			{
				return "test";
			}

			@Override
			public boolean providesMessageTemplatingFunctionality()
			{
				return false;
			}

			@Override
			public Future<NotificationStatus> sendExternalTemplateMessage(String recipientAddress,
					MessageTemplateParams templateParams)
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String getAddressForEntity(EntityParam recipient, String address, boolean confirmedOnly)
			throws EngineException
	{
		throw new IllegalIdentityValueException("no address");
	}

	@Override
	public String getAddressForUserRequest(UserRequestState<?> currentRequest) throws EngineException
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
