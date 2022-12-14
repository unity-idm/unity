/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;

@JsonDeserialize(builder = RestIdentity.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestIdentity
{
	public final long entityId;
	public final Date creationTs;
	public final Date updateTs;
	public final String comparableValue;
	public final String translationProfile;
	public final String remoteIdp;
	public final RestConfirmationInfo confirmationInfo;
	public final JsonNode metadata;
	public final String typeId;
	public final String value;
	public final String target;
	public final String realm;

	private RestIdentity(Builder builder)
	{
		this.entityId = builder.entityId;
		this.creationTs = builder.creationTs;
		this.updateTs = builder.updateTs;
		this.comparableValue = builder.comparableValue;
		this.translationProfile = builder.translationProfile;
		this.remoteIdp = builder.remoteIdp;
		this.confirmationInfo = builder.confirmationInfo;
		this.metadata = builder.metadata;
		this.typeId = builder.typeId;
		this.value = builder.value;
		this.target = builder.target;
		this.realm = builder.realm;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(comparableValue, confirmationInfo, creationTs, entityId, metadata, realm, remoteIdp, target,
				translationProfile, typeId, updateTs, value);
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
		RestIdentity other = (RestIdentity) obj;
		return Objects.equals(comparableValue, other.comparableValue)
				&& Objects.equals(confirmationInfo, other.confirmationInfo)
				&& Objects.equals(creationTs, other.creationTs) && entityId == other.entityId
				&& Objects.equals(metadata, other.metadata) && Objects.equals(realm, other.realm)
				&& Objects.equals(remoteIdp, other.remoteIdp) && Objects.equals(target, other.target)
				&& Objects.equals(translationProfile, other.translationProfile) && Objects.equals(typeId, other.typeId)
				&& Objects.equals(updateTs, other.updateTs) && Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private long entityId;
		private Date creationTs;
		private Date updateTs;
		private String comparableValue;
		private String translationProfile;
		private String remoteIdp;
		private RestConfirmationInfo confirmationInfo;
		private JsonNode metadata;
		private String typeId;
		private String value;
		private String target;
		private String realm;

		private Builder()
		{
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

		public Builder withUpdateTs(Date updateTs)
		{
			this.updateTs = updateTs;
			return this;
		}

		public Builder withComparableValue(String comparableValue)
		{
			this.comparableValue = comparableValue;
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

		public Builder withConfirmationInfo(RestConfirmationInfo confirmationInfo)
		{
			this.confirmationInfo = confirmationInfo;
			return this;
		}

		public Builder withMetadata(JsonNode metadata)
		{
			this.metadata = metadata == null || metadata.isNull() ? null : metadata;
			return this;
		}

		public Builder withTypeId(String typeId)
		{
			this.typeId = typeId;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public Builder withTarget(String target)
		{
			this.target = target;
			return this;
		}

		public Builder withRealm(String realm)
		{
			this.realm = realm;
			return this;
		}

		public RestIdentity build()
		{
			return new RestIdentity(this);
		}
	}

}
