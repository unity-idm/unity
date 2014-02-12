/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.DescribedObjectImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Wraps notification message template. It consist of text, subject may support localization.
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
	private Map<Locale, Message> messagesByLocale;
	private Locale defaultLocale;
	private MessageTemplateConsumer consumer;

	public MessageTemplate(String name, Map<Locale, Message> messagesByLocale,
			Locale defaultLocale, MessageTemplateConsumer consumer)
	{
		this.messagesByLocale = messagesByLocale;
		this.defaultLocale = defaultLocale;
		this.consumer = consumer;
		setName(name);
	}
	
	public MessageTemplate(String json, ObjectMapper jsonMapper, MessageTemplateConsumersRegistry registry)
	{
		fromJson(json, jsonMapper, registry);
	}
	

	private void fromJson(String json, ObjectMapper jsonMapper, MessageTemplateConsumersRegistry registry)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(json);
			setName(root.get("name").asText());
			setDescription(root.get("description").asText());
			ArrayNode messagesA = (ArrayNode) root.get("messages");
			messagesByLocale = new HashMap<Locale, MessageTemplate.Message>();
			for (int i=0; i<messagesA.size(); i++)
			{
				ObjectNode jsonMsg = (ObjectNode) messagesA.get(i);
				Message msg = new Message(jsonMsg.get("subject").asText(), jsonMsg.get("body").asText());
				Locale l = UnityServerConfiguration.safeLocaleDecode(jsonMsg.get("locale").asText());
				messagesByLocale.put(l, msg);
			}
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
			ArrayNode jsonMessages = root.putArray("messages");
			
			for (Map.Entry<Locale, Message> msg: messagesByLocale.entrySet())
			{
				ObjectNode jsonMsg = jsonMessages.addObject();
				jsonMsg.put("locale", msg.getKey().toString());
				jsonMsg.put("subject", msg.getValue().getSubject());
				jsonMsg.put("body", msg.getValue().getBody());
				
			}
			return jsonMapper.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize message template to JSON", e);
		}
		
	}

	public MessageTemplateConsumer getConsumer()
	{
		return this.consumer;
	}
	
	public Message getMessage(Map<String, String> params)
	{
		return getMsg(messagesByLocale,params);
	}
	
	public Message getRawMessage()
	{
		return getMsg(messagesByLocale, new HashMap<String, String>(0));
	}
	
	private Message getMsg(Map<Locale, Message> messagesByLocale, Map<String, String> params)
	{
		Locale loc = UnityMessageSource.getLocale(defaultLocale);
		Message msg = messagesByLocale.get(loc);
		for (Map.Entry<String, String> paramE: params.entrySet())
		{
			msg.setSubject(msg.getSubject().replace("${"+paramE.getKey()+"}", paramE.getValue()));
		        msg.setBody(msg.getBody().replace("${"+paramE.getKey()+"}", paramE.getValue()));
		}
		return msg;
	}
	
		
	public static class Message
	{
		String body;
		String subject;
		
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

		public Message(String subject, String body)
		{
			this.subject = subject;
			this.body = body;
		}
		
		
	}
}
