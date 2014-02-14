/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;
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
	private Map<String, Message> messagesByLocale;
	private String consumer;

	public MessageTemplate(String name, String description,
			Map<String, Message> messagesByLocale, String consumer)
	{
		this.messagesByLocale = messagesByLocale;
		this.consumer = consumer;
		setName(name);
		setDescription(description);
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
			setConsumer(root.get("consumer").asText());
			ArrayNode messagesA = (ArrayNode) root.get("messages");
			messagesByLocale = new HashMap<String, MessageTemplate.Message>();
			for (int i=0; i<messagesA.size(); i++)
			{
				ObjectNode jsonMsg = (ObjectNode) messagesA.get(i);
				Message msg = new Message(jsonMsg.get("subject").asText(), jsonMsg.get("body").asText());
				String l = jsonMsg.get("locale").asText();
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
			root.put("consumer", getConsumer());
			ArrayNode jsonMessages = root.putArray("messages");
			
			for (Map.Entry<String, Message> msg: messagesByLocale.entrySet())
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

	private void setConsumer(String consumer)
	{
		this.consumer = consumer;
		
	}
	
	public String getConsumer()
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
	
	private Message getMsg(Map<String, Message> messagesByLocale, Map<String, String> params)
	{
	//	Locale loc = UnityMessageSource.getLocale(defaultLocale);
		Message msg = messagesByLocale.get("");
		for (Map.Entry<String, String> paramE: params.entrySet())
		{
			msg.setSubject(msg.getSubject().replace("${"+paramE.getKey()+"}", paramE.getValue()));
		        msg.setBody(msg.getBody().replace("${"+paramE.getKey()+"}", paramE.getValue()));
		}
		return msg;
	}
	
	public Map<String, Message> getAllMessages()
	{
		return messagesByLocale;
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
