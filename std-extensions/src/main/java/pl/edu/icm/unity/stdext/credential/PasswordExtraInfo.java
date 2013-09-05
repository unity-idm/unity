/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.io.IOException;
import java.util.Date;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents extra information about an existing password credential, the information
 * which can be read without special security risk.
 * 
 * @author K. Benedyczak
 */
public class PasswordExtraInfo
{
	private Date lastChange;
	private String securityQuestion;
	private int resetTries;
	
	private PasswordExtraInfo()
	{
	}

	public PasswordExtraInfo(Date lastChange, String securityQuestion, int resetTries)
	{
		this.lastChange = lastChange;
		this.securityQuestion = securityQuestion;
		this.resetTries = resetTries;
	}

	public Date getLastChange()
	{
		return lastChange;
	}

	public void setLastChange(Date lastChange)
	{
		this.lastChange = lastChange;
	}

	public String getSecurityQuestion()
	{
		return securityQuestion;
	}

	public void setSecurityQuestion(String securityQuestion)
	{
		this.securityQuestion = securityQuestion;
	}

	public int getResetTries()
	{
		return resetTries;
	}

	public void setResetTries(int resetTries)
	{
		this.resetTries = resetTries;
	}
	
	public static PasswordExtraInfo fromJson(String json)
	{
		PasswordExtraInfo ret = new PasswordExtraInfo();
		if (json == null || json.equals("")) //new credential
			return ret;
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			ret.setLastChange(new Date(root.get("lastChange").asLong()));
			if (root.has("question"))
				ret.setSecurityQuestion(root.get("question").asText());
			ret.setResetTries(root.get("resetTries").asInt());
			return ret;
		}  catch (IOException e)
		{
			throw new InternalException("Can't deserialize extra credential information from JSON", e);
		}
			
	}
	
	public String toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("lastChange", lastChange.getTime());
		if (securityQuestion != null)
			root.put("question", securityQuestion);
		root.put("resetTries", resetTries);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize extra credential information to JSON", e);
		}
	}
}
