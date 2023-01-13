/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;
import java.util.Objects;

@JsonDeserialize(builder = RestProjectMembership.RestProjectMembershipBuilder.class)
class RestProjectMembership
{
	public final String group;
	public final long entityId;
	public final Instant creationTs;
	public final String translationProfile;
	public final String remoteIdp;

	RestProjectMembership(String group, long entityId, Instant creationTs, String translationProfile, String remoteIdp)
	{
		this.group = group;
		this.entityId = entityId;
		this.creationTs = creationTs;
		this.translationProfile = translationProfile;
		this.remoteIdp = remoteIdp;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestProjectMembership that = (RestProjectMembership) o;
		return entityId == that.entityId && Objects.equals(group, that.group) && Objects.equals(creationTs,
			that.creationTs) && Objects.equals(translationProfile, that.translationProfile) &&
			Objects.equals(remoteIdp, that.remoteIdp);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(group, entityId, creationTs, translationProfile, remoteIdp);
	}

	@Override
	public String toString()
	{
		return "RestProjectMembership{" +
			"group='" + group + '\'' +
			", entityId=" + entityId +
			", creationTs=" + creationTs +
			", translationProfile='" + translationProfile + '\'' +
			", remoteIdp='" + remoteIdp + '\'' +
			'}';
	}

	public static RestProjectMembershipBuilder builder()
	{
		return new RestProjectMembershipBuilder();
	}

	public static final class RestProjectMembershipBuilder
	{
		private String group;
		private long entityId;
		private Instant creationTs;
		private String translationProfile;
		private String remoteIdp;

		private RestProjectMembershipBuilder()
		{
		}

		public RestProjectMembershipBuilder withGroup(String group)
		{
			this.group = group;
			return this;
		}

		public RestProjectMembershipBuilder withEntityId(long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public RestProjectMembershipBuilder withCreationTs(Instant creationTs)
		{
			this.creationTs = creationTs;
			return this;
		}

		public RestProjectMembershipBuilder withTranslationProfile(String translationProfile)
		{
			this.translationProfile = translationProfile;
			return this;
		}

		public RestProjectMembershipBuilder withRemoteIdp(String remoteIdp)
		{
			this.remoteIdp = remoteIdp;
			return this;
		}

		public RestProjectMembership build()
		{
			return new RestProjectMembership(group, entityId, creationTs, translationProfile, remoteIdp);
		}
	}
}
