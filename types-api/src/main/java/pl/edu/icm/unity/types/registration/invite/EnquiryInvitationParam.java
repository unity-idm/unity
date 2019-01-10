/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EnquiryInvitationParam extends InvitationParam
{
	private Long entity;
	
	
	public EnquiryInvitationParam(String formId, Instant expiration)
	{
		super(InvitationType.ENQUIRY, formId, expiration);
	}
	
	public EnquiryInvitationParam(String formId, Instant expiration, String contactAddress)
	{
		super(InvitationType.ENQUIRY, formId, expiration, contactAddress);
	}
	
	
	private EnquiryInvitationParam() 
	{
		super(InvitationType.ENQUIRY);
	}
	
	@JsonCreator
	public EnquiryInvitationParam(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}
	
	public Long getEntity()
	{
		return entity;
	}

	public void setEntity(Long entity)
	{
		this.entity = entity;
	}


	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = super.toJson();
		if (getEntity() != null)
			json.put("entity", entity);
		return json;
	}
	
	protected void fromJson(ObjectNode json)
	{
		JsonNode n = json.get("entity");
		if (n != null)
			entity = n.asLong();
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), entity);
	}
	
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnquiryInvitationParam other = (EnquiryInvitationParam) obj;
		return Objects.equals(entity, other.entity);
	}
	
	@Override
	public InvitationParam clone()
	{
		return new EnquiryInvitationParam(this.toJson());
	}
	
	public static Builder builder()
	{
		return new Builder();
	}
	
	public static class Builder extends InvitationParam.Builder<Builder>
	{
		private EnquiryInvitationParam instance ;

		public Builder()
		{
			super(new EnquiryInvitationParam());
			instance = (EnquiryInvitationParam) super.getInstance();
		}
		
		public EnquiryInvitationParam build()
		{
			return instance;
		}
		
		public Builder withEntity(Long entity)
		{
			instance.entity = entity;
			return this;
		}
	}	
}
