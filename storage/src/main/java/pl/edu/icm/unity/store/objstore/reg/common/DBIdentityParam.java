/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.DBConfirmationInfo;

@JsonDeserialize(builder = DBIdentityParam.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DBIdentityParam
{
	public final String translationProfile;
	public final String remoteIdp;
	public final DBConfirmationInfo confirmationInfo;
	public final JsonNode metadata;
	public final String typeId;
	public final String value;
	public final String target;
	public final String realm;

	private DBIdentityParam(Builder builder)
	{
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
		return Objects.hash(confirmationInfo, metadata, realm, remoteIdp, target, translationProfile, typeId, value);
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
		DBIdentityParam other = (DBIdentityParam) obj;
		return Objects.equals(confirmationInfo, other.confirmationInfo) && Objects.equals(metadata, other.metadata)
				&& Objects.equals(realm, other.realm) && Objects.equals(remoteIdp, other.remoteIdp)
				&& Objects.equals(target, other.target) && Objects.equals(translationProfile, other.translationProfile)
				&& Objects.equals(typeId, other.typeId) && Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String translationProfile;
		private String remoteIdp;
		private DBConfirmationInfo confirmationInfo;
		private JsonNode metadata;
		private String typeId;
		private String value;
		private String target;
		private String realm;

		private Builder()
		{
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

		public Builder withConfirmationInfo(DBConfirmationInfo confirmationInfo)
		{
			this.confirmationInfo = confirmationInfo;
			return this;
		}

		public Builder withMetadata(JsonNode metadata)
		{
			this.metadata = metadata;
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

		public DBIdentityParam build()
		{
			return new DBIdentityParam(this);
		}
	}

}
