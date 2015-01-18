/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Confirmation subsystem configuration entry
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationConfiguration
{
	private String typeToConfirm;
	private String nameToConfirm;
	private String notificationChannel;
	private String msgTemplate;
		
	public ConfirmationConfiguration(String typeToConfirm, String nameToConfirm,
			String notificationChannel, String msgTemplate)
	{
		this.typeToConfirm = typeToConfirm;
		this.nameToConfirm = nameToConfirm;
		this.notificationChannel = notificationChannel;
		this.msgTemplate = msgTemplate;
	}

	public String getMsgTemplate()
	{
		return msgTemplate;
	}

	public void setMsgTemplate(String msgTemplate)
	{
		this.msgTemplate = msgTemplate;
	}

	public String getNotificationChannel()
	{
		return notificationChannel;
	}

	public void setNotificationChannel(String notificationChannel)
	{
		this.notificationChannel = notificationChannel;
	}

	public String getNameToConfirm()
	{
		return nameToConfirm;
	}

	public void setNameToConfirm(String nameToConfirm)
	{
		this.nameToConfirm = nameToConfirm;
	}

	public String getTypeToConfirm()
	{
		return typeToConfirm;
	}

	public void setTypeToConfirm(String typeToConfirm)
	{
		this.typeToConfirm = typeToConfirm;
	}

	public ConfirmationConfiguration(String json, ObjectMapper jsonMapper)
	{
		fromJson(json, jsonMapper);
	}

	private void fromJson(String json, ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(json);
			setNameToConfirm(root.get("nameToConfirm").asText());
			setTypeToConfirm(root.get("typeToConfirm").asText());
			setMsgTemplate(root.get("msgTemplate").asText());
			setNotificationChannel(root.get("notificationChannel").asText());

		} catch (Exception e)
		{
			throw new InternalException(
					"Can't deserialize confirmation configuration from JSON", e);
		}

	}

	public String toJson(ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.put("nameToConfirm", getNameToConfirm());
			root.put("typeToConfirm", getTypeToConfirm());
			root.put("msgTemplate", getMsgTemplate());
			root.put("notificationChannel", getNotificationChannel());
			return jsonMapper.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException(
					"Can't serialize confirmation configuration to JSON", e);
		}
	}

}
