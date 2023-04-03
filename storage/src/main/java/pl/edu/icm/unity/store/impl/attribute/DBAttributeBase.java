/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAttributeBase.DBAttributeBaseBuilder.class)
class DBAttributeBase
{
	public final List<String> values;
	public final String translationProfile;
	public final String remoteIdp;

	protected DBAttributeBase(DBAttributeBaseBuilder<?> builder)
	{
		this.values = Optional.ofNullable(builder.values)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.translationProfile = builder.translationProfile;
		this.remoteIdp = builder.remoteIdp;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(remoteIdp, translationProfile, values);
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
		DBAttributeBase other = (DBAttributeBase) obj;
		return Objects.equals(remoteIdp, other.remoteIdp)
				&& Objects.equals(translationProfile, other.translationProfile) && Objects.equals(values, other.values);
	}

	static DBAttributeBaseBuilder<?> builder()
	{
		return new DBAttributeBaseBuilder<>();
	}

	static class DBAttributeBaseBuilder<T extends DBAttributeBaseBuilder<?>>
	{

		private List<String> values = Collections.emptyList();
		private String translationProfile;
		private String remoteIdp;

		protected DBAttributeBaseBuilder()
		{
		}

		@SuppressWarnings("unchecked")	
		public T withValues(List<String> values)
		{
			this.values = Optional.ofNullable(values)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
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

		DBAttributeBase build()
		{
			return new DBAttributeBase(this);
		}
	}

}