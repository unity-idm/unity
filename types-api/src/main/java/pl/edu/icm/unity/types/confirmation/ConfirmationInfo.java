/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.confirmation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;

/**
 * Stores information about confirmation. 
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationInfo
{
	private boolean confirmed;
	private long confirmationDate;
	private int sentRequestAmount;

	public ConfirmationInfo()
	{	
	}
	
	public ConfirmationInfo(int sentRequestAmount)
	{
		this.sentRequestAmount = sentRequestAmount;
	}
	
	public ConfirmationInfo(boolean confirmed)
	{
		this.confirmed = confirmed;
		if (confirmed)
			this.confirmationDate = System.currentTimeMillis();
	}

	@JsonCreator
	public ConfirmationInfo(ObjectNode root)
	{
		fromJson(root);
	}
	
	public void confirm()
	{
		this.confirmed = true;
		this.sentRequestAmount = 0;
		this.confirmationDate = System.currentTimeMillis();
	}
	
	public void incRequestSent()
	{
		confirmationDate = 0;
		confirmed = false;
		sentRequestAmount = sentRequestAmount + 1;
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

	public int getSentRequestAmount()
	{
		return sentRequestAmount;
	}

	public void setSentRequestAmount(int sendedRequestAmount)
	{
		this.sentRequestAmount = sendedRequestAmount;
	}

	@Override
	public String toString()
	{
		return "ConfirmationInfo [confirmed=" + confirmed + ", confirmationDate="
				+ confirmationDate + ", sentRequestAmount=" + sentRequestAmount
				+ "]";
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("confirmed", isConfirmed());
		main.put("confirmationDate", getConfirmationDate());
		main.put("sentRequestAmount", getSentRequestAmount());
		return main;
	}

	
	private void fromJson(ObjectNode jsonN)
	{
		setConfirmed(jsonN.get("confirmed").asBoolean(false));
		setConfirmationDate(jsonN.get("confirmationDate").asLong());
		setSentRequestAmount(jsonN.get("sentRequestAmount").asInt());
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ConfirmationInfo))
			return false;
		ConfirmationInfo other = (ConfirmationInfo) obj;

		if (confirmed != other.isConfirmed())
			return false;
		if (confirmationDate != other.getConfirmationDate())
			return false;
		if (sentRequestAmount != other.getSentRequestAmount())
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
		result = prime * result + sentRequestAmount;
		return result;
	}
}
