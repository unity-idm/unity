/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBInvitationWithCode
{
	public final String registrationCode;
	public final Instant lastSentTime;
	public final Instant creationTime;
	public final int numberOfSends;
	public final DBInvitationParam invitation;

	private DBInvitationWithCode(Builder builder)
	{
		this.registrationCode = builder.registrationCode;
		this.lastSentTime = builder.lastSentTime;
		this.creationTime = builder.creationTime;
		this.numberOfSends = builder.numberOfSends;
		this.invitation = builder.invitation;
	}

	@JsonCreator
	public DBInvitationWithCode(ObjectNode json)
	{
		String type = json.get("type")
				.asText();
		switch (type)
		{
		case DBRegistrationInvitationParam.type:
			invitation = new ObjectMapper().convertValue(json, DBRegistrationInvitationParam.class);
			break;
		case DBEnquiryInvitationParam.type:
			invitation = new ObjectMapper().convertValue(json, DBEnquiryInvitationParam.class);
			break;
		case DBComboInvitationParam.type:
			invitation = new ObjectMapper().convertValue(json, DBComboInvitationParam.class);
			break;
		default:
			throw new IllegalArgumentException("Illegal invitation type");
		}

		registrationCode = json.get("registrationCode")
				.asText();
		if (json.has("lastSentTime"))
		{
			Instant lastSent = Instant.ofEpochMilli(json.get("lastSentTime")
					.asLong());
			lastSentTime = lastSent;
		} else
		{
			lastSentTime = null;
		}

		if (json.has("creationTime"))
		{
			Instant creationTime = Instant.ofEpochMilli(json.get("creationTime")
					.asLong());
			this.creationTime = creationTime;
		} else
		{
			creationTime = null;
		}

		numberOfSends = json.get("numberOfSends")
				.asInt();
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = new ObjectMapper().convertValue(invitation, ObjectNode.class);

		json.put("registrationCode", registrationCode);
		if (lastSentTime != null)
			json.put("lastSentTime", lastSentTime.toEpochMilli());
		if (creationTime != null)
			json.put("creationTime", creationTime.toEpochMilli());
		json.put("numberOfSends", numberOfSends);
		return json;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(creationTime, invitation, lastSentTime, numberOfSends, registrationCode);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBInvitationWithCode other = (DBInvitationWithCode) obj;
		return Objects.equals(creationTime, other.creationTime) && Objects.equals(invitation, other.invitation)
				&& Objects.equals(lastSentTime, other.lastSentTime) && numberOfSends == other.numberOfSends
				&& Objects.equals(registrationCode, other.registrationCode);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String registrationCode;
		private Instant lastSentTime;
		private Instant creationTime;
		private int numberOfSends;
		private DBInvitationParam invitation;

		private Builder()
		{
		}

		public Builder withRegistrationCode(String registrationCode)
		{
			this.registrationCode = registrationCode;
			return this;
		}

		public Builder withLastSentTime(Instant lastSentTime)
		{
			this.lastSentTime = lastSentTime;
			return this;
		}

		public Builder withCreationTime(Instant creationTime)
		{
			this.creationTime = creationTime;
			return this;
		}

		public Builder withNumberOfSends(int numberOfSends)
		{
			this.numberOfSends = numberOfSends;
			return this;
		}

		public Builder withInvitation(DBInvitationParam invitation)
		{
			this.invitation = invitation;
			return this;
		}

		public DBInvitationWithCode build()
		{
			return new DBInvitationWithCode(this);
		}
	}

}
