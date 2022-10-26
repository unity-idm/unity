/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;

@JsonDeserialize(builder = RestAttributeExt.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestAttributeExt
{
	public final boolean direct;
	public final Date creationTs;
	public final Date updateTs;
	public final String name;
	public final String valueSyntax;
	public final String groupPath;
	public final List<String> values;
	public final String translationProfile;
	public final String remoteIdp;

	@Override
	public int hashCode()
	{
		return Objects.hash(creationTs, direct, groupPath, name, remoteIdp, translationProfile, updateTs, valueSyntax,
				values);
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
		RestAttributeExt other = (RestAttributeExt) obj;
		return Objects.equals(creationTs, other.creationTs) && direct == other.direct
				&& Objects.equals(groupPath, other.groupPath) && Objects.equals(name, other.name)
				&& Objects.equals(remoteIdp, other.remoteIdp)
				&& Objects.equals(translationProfile, other.translationProfile)
				&& Objects.equals(updateTs, other.updateTs) && Objects.equals(valueSyntax, other.valueSyntax)
				&& Objects.equals(values, other.values);
	}

	private RestAttributeExt(Builder builder)
	{
		this.direct = builder.direct;
		this.creationTs = builder.creationTs;
		this.updateTs = builder.updateTs;
		this.name = builder.name;
		this.valueSyntax = builder.valueSyntax;
		this.groupPath = builder.groupPath;
		this.values = List.copyOf(builder.values);
		this.translationProfile = builder.translationProfile;
		this.remoteIdp = builder.remoteIdp;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean direct;
		private Date creationTs;
		private Date updateTs;
		private String name;
		private String valueSyntax;
		private String groupPath;
		private List<String> values = Collections.emptyList();
		private String translationProfile;
		private String remoteIdp;

		private Builder()
		{
		}

		public Builder withDirect(boolean direct)
		{
			this.direct = direct;
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
			this.values = values;
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

		public RestAttributeExt build()
		{
			return new RestAttributeExt(this);
		}
	}

}
