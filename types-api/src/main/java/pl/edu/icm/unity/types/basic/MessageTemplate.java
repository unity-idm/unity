/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.DescribedObjectImpl;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;

/**
 * Wraps notification message template. It consist of text, subject, supports localization.
 * What is most important it handles parameter substitution.
 * <p>
 * Implementation note: this will be extended in future: based on freemarker, message metadata (as use HTML) 
 * added etc. For now is simplistic.
 * <p>
 * The syntax:
 * all '${foo}' expressions are replaced by the value of the foo key.
 * 
 * @author K. Benedyczak
 */
public class MessageTemplate extends DescribedObjectImpl
{	
	private I18nMessage message;
	private String consumer;
	private MessageType type;
	private String notificationChannel;

	public MessageTemplate()
	{
	}
	
	public MessageTemplate(String name, String description,
			I18nMessage message, String consumer, MessageType type, String notificationChannel)
	{
		this.message = message;
		this.consumer = consumer;
		this.type = type;
		this.notificationChannel = notificationChannel;
		setName(name);
		setDescription(description);
	}
	
	@JsonCreator
	public MessageTemplate(ObjectNode root)
	{
		fromJson(root);
	}
	

	private void fromJson(ObjectNode root)
	{
		setName(root.get("name").asText());
		setDescription(root.get("description").asText());
		setConsumer(root.get("consumer").asText());
		
		MessageType messageType = MessageType.PLAIN;
		if (JsonUtil.notNull(root, "type"))
			messageType = MessageType.valueOf(root.get("type").asText());
		setType(messageType);
		
		if (JsonUtil.notNull(root, "notificationChannel"))
			setNotificationChannel(root.get("notificationChannel").asText());
		
		ArrayNode messagesA = (ArrayNode) root.get("messages");
		//note: JSON representation is legacy, that's why standard tool to serialize/deserialize 
		//is not used. The empty string was used as an only key to store a default value. 
		I18nString subject = new I18nString();
		I18nString body = new I18nString();
		for (int i=0; i<messagesA.size(); i++)
		{
			ObjectNode jsonMsg = (ObjectNode) messagesA.get(i);
			String locale = jsonMsg.get("locale").asText();
			if (locale.equals(""))
			{
				JsonNode n = jsonMsg.get("subject");
				if (n != null && !n.isNull())
					subject.setDefaultValue(n.asText());
				n = jsonMsg.get("body");
				if (n != null && !n.isNull())
					body.setDefaultValue(n.asText());
			} else
			{
				JsonNode n = jsonMsg.get("subject");
				if (n != null && !n.isNull())
					subject.addValue(locale, n.asText());
				n = jsonMsg.get("body");
				if (n != null && !n.isNull())
					body.addValue(locale, n.asText());
			}
		}
		message = new I18nMessage(subject, body);
	}

	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", getName());
		root.put("description", getDescription());
		root.put("consumer", getConsumer());
		if (getType() != null)
			root.put("type", getType().name());
		ArrayNode jsonMessages = root.putArray("messages");
		root.put("notificationChannel", getNotificationChannel());
		
		I18nString subject = message.getSubject();
		I18nString body = message.getBody();
		Set<String> allUsedLocales = new HashSet<>(body.getMap().keySet());
		allUsedLocales.addAll(subject.getMap().keySet());
		for (String locale: allUsedLocales)
		{
			ObjectNode jsonMsg = jsonMessages.addObject();
			jsonMsg.put("locale", locale);
			jsonMsg.put("subject", subject.getValueRaw(locale));
			jsonMsg.put("body", body.getValueRaw(locale));
		}
		if (subject.getDefaultValue() != null || body.getDefaultValue() != null)
		{
			ObjectNode jsonMsg = jsonMessages.addObject();
			jsonMsg.put("locale", "");
			jsonMsg.put("subject", subject.getDefaultValue());
			jsonMsg.put("body", body.getDefaultValue());
		}
		return root;
	}

	public void setConsumer(String consumer)
	{
		this.consumer = consumer;
	}
	
	public String getConsumer()
	{
		return this.consumer;
	}
	
	public MessageType getType()
	{
		return type;
	}

	public void setType(MessageType type)
	{
		this.type = type;
	}
	
	public String getNotificationChannel()
	{
		return notificationChannel;
	}

	public void setNotificationChannel(String notificationChannel)
	{
		this.notificationChannel = notificationChannel;
	}
	
	public I18nMessage getMessage()
	{
		return message;
	}
	
	public void setMessage(I18nMessage message)
	{
		this.message = message;
	}
	
	/**
	 * Objects are used to interchange resolved messages, with substituted parameters and 
	 * fixed locale.
	 * @author K. Benedyczak
	 */
	public static class Message
	{
		private String body;
		private String subject;
		private MessageType type;
		
		public Message(String subject, String body, MessageType type)
		{
			this.subject = subject;
			this.body = body;
			this.type = type;
		}

		public void setBody(String body)
		{
			this.body = body;
		}

		public void setSubject(String subject)
		{
			this.subject = subject;
		}

		public String getBody()
		{
			return body;
		}

		public String getSubject()
		{
			return subject;
		}
		
		public MessageType getType()
		{
			return type;
		}

		@Override
		public String toString()
		{
			return "Message [body=" + body + ", subject=" + subject + ", type=" + type + "]";
		}

		@Override
		public boolean equals(final Object other)
		{
			if (!(other instanceof Message))
				return false;
			Message castOther = (Message) other;
			return Objects.equals(body, castOther.body) && Objects.equals(subject, castOther.subject)
					&& Objects.equals(type, castOther.type);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(body, subject, type);
		}
	}
	
	
	public MessageTemplate clone()
	{
		ObjectNode json = toJson();
		return new MessageTemplate(json);
	}

	@Override
	public String toString()
	{
		return "MessageTemplate [message=" + message + ", consumer=" + consumer + ", type="
				+ type + ", notificationChannel=" + notificationChannel + "]";
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof MessageTemplate))
			return false;
		if (!super.equals(other))
			return false;
		MessageTemplate castOther = (MessageTemplate) other;
		return Objects.equals(message, castOther.message) && Objects.equals(consumer, castOther.consumer)
				&& Objects.equals(type, castOther.type)
				&& Objects.equals(notificationChannel, castOther.notificationChannel);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), message, consumer, type, notificationChannel);
	}

}
