/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
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

	public MessageTemplate(String name, String description,
			I18nMessage message, String consumer)
	{
		this.message = message;
		this.consumer = consumer;
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

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", getName());
		root.put("description", getDescription());
		root.put("consumer", getConsumer());
		ArrayNode jsonMessages = root.putArray("messages");

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

	private void setConsumer(String consumer)
	{
		this.consumer = consumer;
		
	}
	
	public String getConsumer()
	{
		return this.consumer;
	}
	
	public Message getMessage(String locale, String defaultLocale, Map<String, String> params)
	{
		String subject = message.getSubject().getValue(locale, defaultLocale);
		String body = message.getBody().getValue(locale, defaultLocale);
		Message ret = new Message(subject, body);
		for (Map.Entry<String, String> paramE: params.entrySet())
		{
			if (paramE.getValue() == null)
				continue;
			ret.setSubject(ret.getSubject().replace("${" + paramE.getKey() + "}", paramE.getValue()));
		        ret.setBody(ret.getBody().replace("${" + paramE.getKey() + "}", paramE.getValue()));
		}
		return ret;
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
		
		public Message(String subject, String body)
		{
			this.subject = subject;
			this.body = body;
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

		@Override
		public String toString()
		{
			return "Message [body=" + body + ", subject=" + subject + "]";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((body == null) ? 0 : body.hashCode());
			result = prime * result + ((subject == null) ? 0 : subject.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Message other = (Message) obj;
			if (body == null)
			{
				if (other.body != null)
					return false;
			} else if (!body.equals(other.body))
				return false;
			if (subject == null)
			{
				if (other.subject != null)
					return false;
			} else if (!subject.equals(other.subject))
				return false;
			return true;
		}
	}

	@Override
	public String toString()
	{
		return "MessageTemplate [message=" + message + ", consumer=" + consumer + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((consumer == null) ? 0 : consumer.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageTemplate other = (MessageTemplate) obj;
		if (consumer == null)
		{
			if (other.consumer != null)
				return false;
		} else if (!consumer.equals(other.consumer))
			return false;
		if (message == null)
		{
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}
}
