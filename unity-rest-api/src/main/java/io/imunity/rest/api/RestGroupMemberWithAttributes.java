/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.basic.RestAttributeExt;
import io.imunity.rest.api.types.basic.RestEntityInformation;
import io.imunity.rest.api.types.basic.RestIdentity;

@JsonDeserialize(builder = RestGroupMemberWithAttributes.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestGroupMemberWithAttributes
{
	public final RestEntityInformation entityInformation;
	public final List<RestIdentity> identities;
	public final Collection<RestAttributeExt> attributes;

	private RestGroupMemberWithAttributes(Builder builder)
	{
		this.entityInformation = builder.entityInformation;
		this.identities = List.copyOf(builder.identities);
		this.attributes = List.copyOf(builder.attributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, entityInformation, identities);
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
		RestGroupMemberWithAttributes other = (RestGroupMemberWithAttributes) obj;
		return Objects.equals(attributes, other.attributes)
				&& Objects.equals(entityInformation, other.entityInformation)
				&& Objects.equals(identities, other.identities);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private RestEntityInformation entityInformation;
		private List<RestIdentity> identities = Collections.emptyList();
		private Collection<RestAttributeExt> attributes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withEntityInformation(RestEntityInformation entityInformation)
		{
			this.entityInformation = entityInformation;
			return this;
		}

		public Builder withIdentities(List<RestIdentity> identities)
		{
			this.identities = identities;
			return this;
		}

		public Builder withAttributes(Collection<RestAttributeExt> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public RestGroupMemberWithAttributes build()
		{
			return new RestGroupMemberWithAttributes(this);
		}
	}

}
