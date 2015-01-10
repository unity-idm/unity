/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.JsonSerializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains all confirmation data
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationData implements JsonSerializable
{
	private boolean confirmed;
	private long confirmationDate;
	private int sendedRequestAmount;

	public ConfirmationData()
	{	
	}
	public ConfirmationData(int sendedRequestAmount)
	{
		this.sendedRequestAmount = sendedRequestAmount;
	}
	
	public boolean isConfirmed()
	{
		return confirmed;
	}

	public void setConfirmed(boolean confirmed)
	{
		this.confirmed = confirmed;
	}

	public long getConfirmationDate()
	{
		return confirmationDate;
	}

	public void setConfirmationDate(long confirmationDate)
	{
		this.confirmationDate = confirmationDate;
	}

	public int getSendedRequestAmount()
	{
		return sendedRequestAmount;
	}

	public void setSendedRequestAmount(int sendedRequestAmount)
	{
		this.sendedRequestAmount = sendedRequestAmount;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("confirmed", isConfirmed());
		main.put("confirmationDate", getConfirmationDate());
		main.put("sendedRequestAmount", getSendedRequestAmount());
		try
		{
			return Constants.MAPPER.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize ConfirmationData to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		JsonNode jsonN;
		try
		{
			jsonN = Constants.MAPPER.readTree(new String(json));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize ConfirmationData from JSON",
					e);
		}

		setConfirmed(jsonN.get("confirmed").asBoolean(false));
		setConfirmationDate(jsonN.get("confirmationDate").asLong());
		setSendedRequestAmount(jsonN.get("sendedRequestAmount").asInt());
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ConfirmationData))
			return false;
		ConfirmationData other = (ConfirmationData) obj;

		if (confirmed != other.isConfirmed())
			return false;
		if (confirmationDate != other.getConfirmationDate())
			return false;
		if (sendedRequestAmount != other.getSendedRequestAmount())
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (confirmationDate ^ (confirmationDate >>> 32));
		result = prime * result + (confirmed ? 1231 : 1237);
		result = prime * result + sendedRequestAmount;
		return result;
	}
}
