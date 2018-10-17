/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.confirmation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;

/**
 * Email confirmation subsystem configuration entry
 * 
 * @author P. Piernik
 * 
 */
public class EmailConfirmationConfiguration
{	
	public static final int DEFAULT_VALIDITY = 48*60;
	
	private String messageTemplate;	
	private int validityTime = DEFAULT_VALIDITY;

	
	public EmailConfirmationConfiguration()
	{
	}
	
	public EmailConfirmationConfiguration(String messageTemplate)
	{
		this.messageTemplate = messageTemplate;
	}
	
	@JsonCreator
	public EmailConfirmationConfiguration(ObjectNode root)
	{
		fromJson(root);
	}

	public int getValidityTime()
	{
		return validityTime;
	}
	
	public void setValidityTime(int validityTime)
	{
		this.validityTime = validityTime;
	}

	public String getMessageTemplate()
	{
		return messageTemplate;
	}

	public void setMessageTemplate(String messageTemplate)
	{
		this.messageTemplate = messageTemplate;
	}
	
	private void fromJson(ObjectNode root)
	{	
		if (JsonUtil.notNull(root, "messageTemplate")) 
			setMessageTemplate(root.get("messageTemplate").asText());
		if (JsonUtil.notNull(root, "validityTime")) 
			setValidityTime(root.get("validityTime").asInt());	
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("messageTemplate", getMessageTemplate());
		root.put("validityTime", getValidityTime());
		return root;
	}
}
