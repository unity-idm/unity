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
 * Mobile number confirmation subsystem configuration entry
 * 
 * @author P. Piernik
 * 
 */
public class MobileNumberConfirmationConfiguration
{	
	public static final int DEFAULT_VALIDITY = 15;
	public static final int DEFAULT_CODE_LENGTH = 6;
	
	private String messageTemplate;	
	private int validityTime = DEFAULT_VALIDITY;
	private int codeLength = DEFAULT_CODE_LENGTH;

	
	public MobileNumberConfirmationConfiguration()
	{
	}
	
	public MobileNumberConfirmationConfiguration(String messageTemplate)
	{
		this.messageTemplate = messageTemplate;
	}
	
	@JsonCreator
	public MobileNumberConfirmationConfiguration(ObjectNode root)
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
	
	public int getCodeLength()
	{
		return codeLength;
	}

	public void setCodeLength(int codeLength)
	{
		this.codeLength = codeLength;
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
		if (JsonUtil.notNull(root, "codeLength")) 
			setCodeLength(root.get("codeLength").asInt());
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("messageTemplate", getMessageTemplate());
		root.put("validityTime", getValidityTime());
		root.put("codeLength", getCodeLength());
		return root;
	}
}
