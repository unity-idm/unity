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
 * In DB representation of the sms credential state.
 * 
 * @author P.Piernik
 */
class SMSCredentialDBState
{
	private String value;
	private Date time;

	public SMSCredentialDBState(String value, long time)
	{
		this.value = value;
		this.setTime(new Date(time));
	}

	public SMSCredentialDBState()
	{

	}

	public static SMSCredentialDBState fromJson(String raw) throws InternalException
	{
		if (raw == null || raw.length() == 0)
			return new SMSCredentialDBState(null, System.currentTimeMillis());
		try
		{
			return Constants.MAPPER.readValue(raw, SMSCredentialDBState.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize sms credential from JSON",
					e);
		}
	}

	public static String toJson(SMSCredential credential, String value, long time)
	{

		SMSCredentialDBState dbState = new SMSCredentialDBState(value, time);
		try
		{
			return Constants.MAPPER.writeValueAsString(dbState);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize sms credential to JSON", e);
		}
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public Date getTime()
	{
		return time;
	}

	public void setTime(Date time)
	{
		this.time = time;
	}
}
