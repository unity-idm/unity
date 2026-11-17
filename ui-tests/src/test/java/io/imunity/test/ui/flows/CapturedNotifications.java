package io.imunity.test.ui.flows;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;

public final class CapturedNotifications
{
	private final List<Message> messages = new CopyOnWriteArrayList<>();

	void add(CommunicationTechnology technology, String recipient, MessageTemplate.Message message)
	{
		messages.add(new Message(technology, recipient, message.getSubject(), message.getBody()));
	}

	public List<Message> sentTo(String recipient)
	{
		return messages.stream().filter(message -> message.recipient().equals(recipient)).toList();
	}

	public record Message(CommunicationTechnology technology, String recipient, String subject, String body)
	{
	}
}
