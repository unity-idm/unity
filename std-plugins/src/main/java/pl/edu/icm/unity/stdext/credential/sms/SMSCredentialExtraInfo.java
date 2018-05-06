/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Represents extra information about an existing sms credential, the information
 * which can be read without special security risk.
 * 
 * @author P.Piernik
 *
 */
public class SMSCredentialExtraInfo
{
	private Date lastChange;
	private String mobile;
	
	public SMSCredentialExtraInfo(Date lastChange, String mobile)
	{
		this.lastChange = lastChange;
		this.mobile = mobile;
	}

	public SMSCredentialExtraInfo()
	{
	}

	public Date getLastChange()
	{
		return lastChange;
	}

	public void setLastChange(Date lastChange)
	{
		this.lastChange = lastChange;
	}

	public String getMobile()
	{
		return mobile;
	}

	public void setMobile(String mobile)
	{
		this.mobile = mobile;
	}

	public static SMSCredentialExtraInfo fromJson(String json)
	{
		SMSCredentialExtraInfo ret = new SMSCredentialExtraInfo();
		if (json == null || json.equals("")) // new credential
			return ret;
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			ret.setLastChange(new Date(root.get("lastChange").asLong()));
			ret.setMobile(root.get("mobile").asText());
			return ret;
		} catch (IOException e)
		{
			throw new InternalException(
					"Can't deserialize extra credential information from JSON",
					e);
		}
	}

	public String toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("lastChange", lastChange.getTime());
		root.put("mobile", mobile);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException(
					"Can't serialize extra credential information to JSON", e);
		}
	}	
}
