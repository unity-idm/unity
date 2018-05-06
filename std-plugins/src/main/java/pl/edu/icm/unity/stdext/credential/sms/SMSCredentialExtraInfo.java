/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;

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
		if (json == null || json.length() == 0)
			return new SMSCredentialExtraInfo();
		try
		{
			return Constants.MAPPER.readValue(json, SMSCredentialExtraInfo.class);
		} catch (Exception e)
		{
			throw new InternalException(
					"Can't deserialize extra credential information from JSON",
					e);
		}
	}

	public String toJson()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e)
		{
			throw new InternalException(
					"Can't serialize extra credential information to JSON", e);
		}
	}
}
