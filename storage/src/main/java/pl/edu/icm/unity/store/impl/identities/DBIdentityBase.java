/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identities;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;

@JsonDeserialize(builder = DBIdentityBase.DBIdentityBaseBuilder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DBIdentityBase
{
	public final Date creationTs;
	public final Date updateTs;
	public final String comparableValue;
	public final String translationProfile;
	public final String remoteIdp;
	public final DBConfirmationInfo confirmationInfo;
	public final JsonNode metadata;
	public final String value;
	public final String target;
	public final String realm;

	protected DBIdentityBase(DBIdentityBaseBuilder<?> builder)
	{
		this.creationTs = builder.creationTs;
		this.updateTs = builder.updateTs;
		this.comparableValue = builder.comparableValue;
		this.translationProfile = builder.translationProfile;
		this.remoteIdp = builder.remoteIdp;
		this.confirmationInfo = builder.confirmationInfo;
		this.metadata = builder.metadata;
		this.value = builder.value;
		this.target = builder.target;
		this.realm = builder.realm;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(comparableValue, confirmationInfo, creationTs, metadata, realm, remoteIdp, target,
				translationProfile, updateTs, value);
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
		DBIdentityBase other = (DBIdentityBase) obj;
		return Objects.equals(comparableValue, other.comparableValue)
				&& Objects.equals(confirmationInfo, other.confirmationInfo)
				&& Objects.equals(creationTs, other.creationTs) 
				&& Objects.equals(metadata, other.metadata) && Objects.equals(realm, other.realm)
				&& Objects.equals(remoteIdp, other.remoteIdp) && Objects.equals(target, other.target)
				&& Objects.equals(translationProfile, other.translationProfile) 
				&& Objects.equals(updateTs, other.updateTs) && Objects.equals(value, other.value);
	}

	public static DBIdentityBaseBuilder<?> builder()
	{
		return new DBIdentityBaseBuilder<>();
	}

	public static  class DBIdentityBaseBuilder<T extends DBIdentityBaseBuilder<?>>
	{
		private Date creationTs;
		private Date updateTs;
		private String comparableValue;
		private String translationProfile;
		private String remoteIdp;
		private DBConfirmationInfo confirmationInfo;
		private JsonNode metadata;
		private String value;
		private String target;
		private String realm;

		protected DBIdentityBaseBuilder()
		{
		}

		

		@SuppressWarnings("unchecked")
		public T withCreationTs(Date creationTs)
		{
			this.creationTs = creationTs;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withUpdateTs(Date updateTs)
		{
			this.updateTs = updateTs;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withComparableValue(String comparableValue)
		{
			this.comparableValue = comparableValue;
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

		@SuppressWarnings("unchecked")
		public T withConfirmationInfo(DBConfirmationInfo confirmationInfo)
		{
			this.confirmationInfo = confirmationInfo;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMetadata(JsonNode metadata)
		{
			this.metadata = metadata == null || metadata.isNull() ? null : metadata;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withValue(String value)
		{
			this.value = value;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withTarget(String target)
		{
			this.target = target;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withRealm(String realm)
		{
			this.realm = realm;
			return (T) this;
		}

		public DBIdentityBase build()
		{			
			return new DBIdentityBase(this);
		}
	}

}
