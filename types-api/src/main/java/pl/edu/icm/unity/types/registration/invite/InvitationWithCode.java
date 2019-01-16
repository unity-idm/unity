/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.NamedObject;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;

/**
 * Complete invitation as stored in the system. 
 * This class is a common base for backend and REST API variants which store attributes differently.
 *   
 * @author Krzysztof Benedyczak
 */
public class InvitationWithCode implements NamedObject
{
	private String registrationCode;
	private Instant lastSentTime;
	private Instant creationTime;
	private int numberOfSends;
	private InvitationParam invitation;

	public InvitationWithCode(InvitationParam base,
			String registrationCode)
	{
		this.invitation = base;
		this.registrationCode = registrationCode;
	}

	public InvitationWithCode(InvitationParam base, String registrationCode,
			Instant lastSentTime, int numberOfSends)
	{
		this.registrationCode = registrationCode;
		this.lastSentTime = lastSentTime;
		this.numberOfSends = numberOfSends;
		invitation = base;	
	}

	@JsonCreator
	public InvitationWithCode(ObjectNode json)
	{
		InvitationType type = InvitationType.valueOf(json.get("type").asText());
		if (type.equals(InvitationType.REGISTRATION))
		{
			invitation = new RegistrationInvitationParam(json);
		}
		else
		{
			invitation = new EnquiryInvitationParam(json);
		}	
		fromJson(json);
	}
	
	public InvitationParam getInvitation()
	{
		return invitation;
	}

	public void setInvitation(InvitationParam invitation)
	{
		this.invitation = invitation;
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

	public Instant getCreationTime()
	{
		return creationTime;
	}

	public void setCreationTime(Instant creationTime)
	{
		this.creationTime = creationTime;
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = invitation.toJson();
		
		json.put("registrationCode", getRegistrationCode());
		if (getLastSentTime() != null)
			json.put("lastSentTime", getLastSentTime().toEpochMilli());
		if (getCreationTime() != null)
			json.put("creationTime", getCreationTime().toEpochMilli());
		json.put("numberOfSends", getNumberOfSends());
		return json;
	}
	
	private void fromJson(ObjectNode json)
	{
		registrationCode = json.get("registrationCode").asText();
		if (json.has("lastSentTime"))
		{
			Instant lastSent = Instant.ofEpochMilli(json.get("lastSentTime").asLong());
			setLastSentTime(lastSent);
		}
		
		if (json.has("creationTime"))
		{
			Instant creationTime = Instant.ofEpochMilli(json.get("creationTime").asLong());
			setCreationTime(creationTime);
		}
		
		setNumberOfSends(json.get("numberOfSends").asInt());
	}

	@Override
	public String getName()
	{
		return getRegistrationCode();
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
		result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
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
		if (getClass() != obj.getClass())
			return false;
		
		InvitationWithCode other = (InvitationWithCode) obj;
		if (invitation == null)
		{
			if (other.invitation != null)
				return false;
		} else if (!invitation.equals(other.invitation))
			return false;
		if (lastSentTime == null)
		{
			if (other.lastSentTime != null)
				return false;
		} else if (!lastSentTime.equals(other.lastSentTime))
			return false;
		if (creationTime == null)
		{
			if (other.creationTime != null)
				return false;
		} else if (!creationTime.equals(other.creationTime))
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



