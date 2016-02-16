/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.msgtemplates;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.DescribedObjectImpl;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	
	public MessageTemplate(String json, ObjectMapper jsonMapper)
	{
		fromJson(json, jsonMapper);
	}
	

	private void fromJson(String json, ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(json);
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
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize message template from JSON", e);
		}

	}

	public String toJson(ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
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
			return jsonMapper.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize message template to JSON", e);
		}
		
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
	}
}
