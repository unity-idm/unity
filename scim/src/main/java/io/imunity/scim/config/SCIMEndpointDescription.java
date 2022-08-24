/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

public class SCIMEndpointDescription
{
	public final URI baseLocation;
	public final String rootGroup;
	public final List<String> membershipGroups;
	public final List<String> excludedMembershipGroups;
	public final List<SchemaWithMapping> schemas;
	public final List<String> membershipAttributes;
	public final List<String> authenticationOptions;

	private SCIMEndpointDescription(Builder builder)
	{
		this.baseLocation = builder.baseLocation;
		this.rootGroup = builder.rootGroup;
		this.membershipGroups = List.copyOf(builder.membershipGroups);
		this.excludedMembershipGroups = List.copyOf(builder.excludedMembershipGroups);
		this.schemas = List.copyOf(builder.schemas);
		this.membershipAttributes = List.copyOf(builder.membershipAttributes);
		this.authenticationOptions = List.copyOf(builder.authenticationOptions);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(baseLocation, membershipAttributes, membershipGroups, excludedMembershipGroups, rootGroup, schemas, authenticationOptions);
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
		SCIMEndpointDescription other = (SCIMEndpointDescription) obj;
		return Objects.equals(baseLocation, other.baseLocation)
				&& Objects.equals(membershipAttributes, other.membershipAttributes)
				&& Objects.equals(membershipGroups, other.membershipGroups)
				&& Objects.equals(excludedMembershipGroups, other.excludedMembershipGroups)
				&& Objects.equals(rootGroup, other.rootGroup) && Objects.equals(schemas, other.schemas)
				&& Objects.equals(authenticationOptions, other.authenticationOptions);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private URI baseLocation;
		private String rootGroup;
		private List<String> membershipGroups = Collections.emptyList();
		private List<String> excludedMembershipGroups = Collections.emptyList();
		private List<SchemaWithMapping> schemas = Collections.emptyList();
		private List<String> membershipAttributes = Collections.emptyList();
		private List<String> authenticationOptions = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withBaseLocation(URI baseLocation)
		{
			this.baseLocation = baseLocation;
			return this;
		}

		public Builder withRootGroup(String rootGroup)
		{
			this.rootGroup = rootGroup;
			return this;
		}

		public Builder withMembershipGroups(List<String> membershipGroups)
		{
			this.membershipGroups = membershipGroups;
			return this;
		}

		public Builder withExcludedMembershipGroups(List<String> excludedMembershipGroups)
		{
			this.excludedMembershipGroups = excludedMembershipGroups;
			return this;
		}

		public Builder withSchemas(List<SchemaWithMapping> schemas)
		{
			this.schemas = schemas;
			return this;
		}

		public Builder withMembershipAttributes(List<String> membershipAttributes)
		{
			this.membershipAttributes = membershipAttributes;
			return this;
		}

		public Builder withAuthenticationOptions(List<String> authenticationOptions)
		{
			this.authenticationOptions = authenticationOptions;
			return this;
		}

		public SCIMEndpointDescription build()
		{
			return new SCIMEndpointDescription(this);
		}
	}
	
	
	

}
