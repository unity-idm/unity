/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.BaseForm;

/**
 * Base data of invitation parameter. It is extracted as we have two ways to represent attributes:
 * one simple for JSON API and one with resolved Attribute for the backend.
 * @author Krzysztof Benedyczak
 */
public abstract class InvitationParam
{
	public static enum InvitationType { ENQUIRY, REGISTRATION, COMBO};
		
	private InvitationType type;
	private Instant expiration;
	private String contactAddress;
	
	protected InvitationParam(InvitationType type) 
	{
		this.type = type;
	}
	
	public InvitationParam(InvitationType type, Instant expiration, String contactAddress)
	{
		this(type, expiration);
		this.contactAddress = contactAddress;
	}

	public InvitationParam(InvitationType type, Instant expiration)
	{
		this.type = type;
		this.expiration = expiration;		
	}

	@JsonCreator
	public InvitationParam(ObjectNode json)
	{
		fromJsonBase(json);
	}
	
	public InvitationType getType()
	{
		return type;
	}

	public void setType(InvitationType type)
	{
		this.type = type;
	}

	public Instant getExpiration()
	{
		return expiration;
	}

	public String getContactAddress()
	{
		return contactAddress;
	}

	public abstract void validateUpdate(InvitationValidator validator, InvitationParam toUpdate) throws EngineException;
	public abstract void validate(InvitationValidator validator) throws EngineException;
	public abstract void send(InvitationSender sender, String code) throws EngineException;
	public abstract boolean matchForm(BaseForm form);
	public abstract FormPrefill getPrefillForForm(BaseForm form) throws EngineException;
	public abstract FormPrefill getPrefillForAutoProcessing();
	public abstract List<FormPrefill> getFormsPrefillData();
	
	@JsonIgnore
	public boolean isExpired()
	{
		return Instant.now().isAfter(getExpiration());
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = Constants.MAPPER.createObjectNode();
		
		json.put("type", getType().toString());
		json.put("expiration", getExpiration().toEpochMilli());
		if (getContactAddress() != null)
			json.put("contactAddress", getContactAddress());
		return json;
	}
	
	private void fromJsonBase(ObjectNode json)
	{
		
		JsonNode n;
		n=json.get("type");
		if (n != null && !n.isNull())
		{
			type = InvitationType.valueOf(json.get("type").asText());	
		}else
		{
			type = InvitationType.REGISTRATION;	
		}
			
		expiration = Instant.ofEpochMilli(json.get("expiration").asLong());
		contactAddress = JsonUtil.getNullable(json, "contactAddress");
		
		
	}
	
	@Override
	public String toString()
	{
		return "InvitationParam [type=" + type +  ", expiration=" + expiration
				+ ", contactAddress=" + contactAddress;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof InvitationParam))
			return false;
		InvitationParam castOther = (InvitationParam) other;
		return  Objects.equals(type, castOther.type) &&
				Objects.equals(expiration, castOther.expiration)
				&& Objects.equals(contactAddress, castOther.contactAddress);
				
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(expiration, contactAddress);
	}

	protected static class Builder<T extends Builder<?>> {
	
		private InvitationParam instance;

		protected Builder(InvitationParam aInstance)
		{
			instance = aInstance;
		}

		protected InvitationParam getInstance()
		{
			return instance;
		}
			
		@SuppressWarnings("unchecked")
		public  T  withExpiration(Instant expiration)
		{
			instance.expiration = expiration;
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		public  T  withContactAddress(String contactAddress)
		{
			instance.contactAddress = contactAddress;
			return (T) this;
		}
	}
}
