/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Complete invitation as stored in the system. 
 * This class is a common base for backend and REST API variants which store attributes differently.
 *   
 * @author Krzysztof Benedyczak
 */
abstract class InvitationWithCodeBase extends InvitationParamBase
{
	private String registrationCode;
	private Instant lastSentTime;
	private int numberOfSends;

	public InvitationWithCodeBase(String formId, Instant expiration, String contactAddress,
			String facilityId, String registrationCode)
	{
		super(formId, expiration, contactAddress, facilityId);
		this.registrationCode = registrationCode;
	}

	public InvitationWithCodeBase(InvitationParamBase base, String registrationCode,
			Instant lastSentTime, int numberOfSends)
	{
		super(base.getFormId(), base.getExpiration(), base.getContactAddress(), base.getChannelId());
		this.registrationCode = registrationCode;
		this.lastSentTime = lastSentTime;
		this.numberOfSends = numberOfSends;
		this.getIdentities().putAll(base.getIdentities());
		this.getGroupSelections().putAll(base.getGroupSelections());
	}

	public InvitationWithCodeBase(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}
	
	public String getRegistrationCode()
	{
		return registrationCode;
	}

	public Instant getLastSentTime()
	{
		return lastSentTime;
	}

	public int getNumberOfSends()
	{
		return numberOfSends;
	}
	
	public void setLastSentTime(Instant lastSentTime)
	{
		this.lastSentTime = lastSentTime;
	}

	public void setNumberOfSends(int numberOfSends)
	{
		this.numberOfSends = numberOfSends;
	}

	@Override
	public ObjectNode toJson()
	{
		ObjectNode json = super.toJson();
		
		json.put("registrationCode", getRegistrationCode());
		if (getLastSentTime() != null)
			json.put("lastSentTime", getLastSentTime().getEpochSecond());
		json.put("numberOfSends", getNumberOfSends());
		return json;
	}
	
	private void fromJson(ObjectNode json)
	{
		registrationCode = json.get("registrationCode").asText();
		if (json.has("lastSentTime"))
		{
			Instant lastSent = Instant.ofEpochSecond(json.get("lastSentTime").asLong());
			setLastSentTime(lastSent);
		}
		setNumberOfSends(json.get("numberOfSends").asInt());
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " [registrationCode=" + registrationCode + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((lastSentTime == null) ? 0 : lastSentTime.hashCode());
		result = prime * result + numberOfSends;
		result = prime * result
				+ ((registrationCode == null) ? 0 : registrationCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvitationWithCodeBase other = (InvitationWithCodeBase) obj;
		if (lastSentTime == null)
		{
			if (other.lastSentTime != null)
				return false;
		} else if (!lastSentTime.equals(other.lastSentTime))
			return false;
		if (numberOfSends != other.numberOfSends)
			return false;
		if (registrationCode == null)
		{
			if (other.registrationCode != null)
				return false;
		} else if (!registrationCode.equals(other.registrationCode))
			return false;
		return true;
	}
}



