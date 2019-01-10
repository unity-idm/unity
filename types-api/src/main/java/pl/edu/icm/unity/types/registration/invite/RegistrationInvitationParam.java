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

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;

/**
 * 
 * @author P.Piernik
 *
 */
public class RegistrationInvitationParam extends InvitationParam
{
	private ExpectedIdentity expectedIdentity;
	
	public RegistrationInvitationParam(String formId, Instant expiration)
	{
		super(InvitationType.REGISTRATION, formId, expiration);
	}
	
	public RegistrationInvitationParam(String formId, Instant expiration, String contactAddress)
	{
		super(InvitationType.REGISTRATION, formId, expiration, contactAddress);
	}
	
	private RegistrationInvitationParam() 
	{
		super(InvitationType.REGISTRATION);
	}
	
	@JsonCreator
	public RegistrationInvitationParam(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}
	
	public ExpectedIdentity getExpectedIdentity()
	{
		return expectedIdentity;
	}

	public void setExpectedIdentity(ExpectedIdentity expectedIdentity)
	{
		this.expectedIdentity = expectedIdentity;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = super.toJson();
		if (getExpectedIdentity() != null)
			json.putPOJO("expectedIdentity", expectedIdentity);
		return json;
	}
	
	protected void fromJson(ObjectNode json)
	{
		JsonNode n = json.get("expectedIdentity");
		if (n != null)
			expectedIdentity = Constants.MAPPER.convertValue(n, ExpectedIdentity.class);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), expectedIdentity);
	}
	
	@Override
	public InvitationParam clone()
	{
		return new RegistrationInvitationParam(this.toJson());
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
		RegistrationInvitationParam other = (RegistrationInvitationParam) obj;
		return Objects.equals(expectedIdentity, other.expectedIdentity);
	}
	
	public static Builder builder()
	{
		return new Builder();
	}
	
	public static class Builder extends InvitationParam.Builder<Builder>
	{
		private RegistrationInvitationParam instance ;

		public Builder()
		{
			super(new RegistrationInvitationParam());
			instance = (RegistrationInvitationParam) super.getInstance();
		}
		
		public RegistrationInvitationParam build()
		{
			return instance;
		}
		
		public Builder withExpectedIdentity(ExpectedIdentity identity)
		{
			instance.expectedIdentity = identity;
			return this;
		}
	}

	
}
