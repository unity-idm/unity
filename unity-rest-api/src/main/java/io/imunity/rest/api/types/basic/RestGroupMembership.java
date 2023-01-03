/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroupMembership.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestGroupMembership
{
	public final String group;
	public final long entityId;
	public final Date creationTs;
	public final String translationProfile;
	public final String remoteIdp;

	private RestGroupMembership(Builder builder)
	{
		this.group = builder.group;
		this.entityId = builder.entityId;
		this.creationTs = builder.creationTs;
		this.translationProfile = builder.translationProfile;
		this.remoteIdp = builder.remoteIdp;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(creationTs, entityId, group, remoteIdp, translationProfile);
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
		RestGroupMembership other = (RestGroupMembership) obj;
		return Objects.equals(creationTs, other.creationTs) && entityId == other.entityId
				&& Objects.equals(group, other.group) && Objects.equals(remoteIdp, other.remoteIdp)
				&& Objects.equals(translationProfile, other.translationProfile);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String group;
		private long entityId;
		private Date creationTs;
		private String translationProfile;
		private String remoteIdp;

		private Builder()
		{
		}

		public Builder withGroup(String group)
		{
			this.group = group;
			return this;
		}

		public Builder withEntityId(long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public Builder withCreationTs(Date creationTs)
		{
			this.creationTs = creationTs;
			return this;
		}

		public Builder withTranslationProfile(String translationProfile)
		{
			this.translationProfile = translationProfile;
			return this;
		}

		public Builder withRemoteIdp(String remoteIdp)
		{
			this.remoteIdp = remoteIdp;
			return this;
		}

		public RestGroupMembership build()
		{
			return new RestGroupMembership(this);
		}
	}

}
