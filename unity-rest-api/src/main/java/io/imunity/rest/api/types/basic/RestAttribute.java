/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;

@JsonDeserialize(builder = RestAttribute.Builder.class)
public class RestAttribute
{
	public final String name;
	public final String valueSyntax;
	public final String groupPath;
	public final List<String> values;
	public final String translationProfile;
	public final String remoteIdp;

	private RestAttribute(Builder builder)
	{
		this.name = builder.name;
		this.valueSyntax = builder.valueSyntax;
		this.groupPath = builder.groupPath;
		this.values = Optional.ofNullable(builder.values).map(Collections::unmodifiableList).orElse(null);
		this.translationProfile = builder.translationProfile;
		this.remoteIdp = builder.remoteIdp;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(groupPath, name, remoteIdp, translationProfile, valueSyntax, values);
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
		RestAttribute other = (RestAttribute) obj;
		return Objects.equals(groupPath, other.groupPath) && Objects.equals(name, other.name)
				&& Objects.equals(remoteIdp, other.remoteIdp)
				&& Objects.equals(translationProfile, other.translationProfile)
				&& Objects.equals(valueSyntax, other.valueSyntax) && Objects.equals(values, other.values);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String valueSyntax;
		private String groupPath;
		private List<String> values = Collections.emptyList();
		private String translationProfile;
		private String remoteIdp;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withValueSyntax(String valueSyntax)
		{
			this.valueSyntax = valueSyntax;
			return this;
		}

		public Builder withGroupPath(String groupPath)
		{
			this.groupPath = groupPath;
			return this;
		}

		public Builder withValues(List<String> values)
		{
			this.values = Optional.ofNullable(values).map(Collections::unmodifiableList).orElse(null);
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

		public RestAttribute build()
		{
			return new RestAttribute(this);
		}
	}

}
