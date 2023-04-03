/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
{ @JsonSubTypes.Type(value = DBRegistrationInvitationParam.class, name = "REGISTRATION"),
		@JsonSubTypes.Type(value = DBEnquiryInvitationParam.class, name = "ENQUIRY"),
		@JsonSubTypes.Type(value = DBComboInvitationParam.class, name = "COMBO") })
public abstract class DBInvitationParam
{
	public final String type;
	public final Long expiration;
	public final String contactAddress;
	public final Long inviter;

	protected DBInvitationParam(RestInvitationParamBuilder<?> builder)
	{
		this.type = builder.type;
		this.expiration = builder.expiration;
		this.contactAddress = builder.contactAddress;
		this.inviter = builder.inviter;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(contactAddress, expiration, inviter, type);
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
		DBInvitationParam other = (DBInvitationParam) obj;
		return Objects.equals(contactAddress, other.contactAddress) && Objects.equals(expiration, other.expiration)
				&& Objects.equals(inviter, other.inviter) && Objects.equals(type, other.type);
	}

	public static class RestInvitationParamBuilder<T extends RestInvitationParamBuilder<?>>
	{
		private String type;
		private Long expiration;
		private String contactAddress;
		private Long inviter;

		protected RestInvitationParamBuilder(String type)
		{
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		public T withType(String type)
		{
			this.type = type;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withExpiration(Long expiration)
		{
			this.expiration = expiration;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withContactAddress(String contactAddress)
		{
			this.contactAddress = contactAddress;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withInviter(Long inviterEntity)
		{
			this.inviter = inviterEntity;
			return (T) this;
		}
	}

}
