/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBGroupMembershipBase.DBGroupMembershipBaseBuilder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class DBGroupMembershipBase
{
	
	final Date creationTs;
	final String translationProfile;
	final String remoteIdp;

	protected DBGroupMembershipBase(DBGroupMembershipBaseBuilder<?> builder)
	{
		this.creationTs = builder.creationTs;
		this.translationProfile = builder.translationProfile;
		this.remoteIdp = builder.remoteIdp;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(creationTs, remoteIdp, translationProfile);
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
		DBGroupMembershipBase other = (DBGroupMembershipBase) obj;
		return Objects.equals(creationTs, other.creationTs) 
				&& Objects.equals(remoteIdp, other.remoteIdp)
				&& Objects.equals(translationProfile, other.translationProfile);
	}

	public static DBGroupMembershipBaseBuilder<?> builder()
	{
		return new DBGroupMembershipBaseBuilder<>();
	}

	public static class DBGroupMembershipBaseBuilder<T extends DBGroupMembershipBaseBuilder<?>>
	{
		
		private Date creationTs;
		private String translationProfile;
		private String remoteIdp;

		protected DBGroupMembershipBaseBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withCreationTs(Date creationTs)
		{
			this.creationTs = creationTs;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withTranslationProfile(String translationProfile)
		{
			this.translationProfile = translationProfile;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withRemoteIdp(String remoteIdp)
		{
			this.remoteIdp = remoteIdp;
			return (T) this;
		}

		public DBGroupMembershipBase build()
		{
			return new DBGroupMembershipBase(this);
		}
	}

}
