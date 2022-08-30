/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SCIMEndpointConfiguration
{
	public final List<String> allowedCorsHeaders;
	public final List<String> allowedCorsOrigins;
	public final String rootGroup;
	public final String restAdminGroup;
	public final List<String> membershipGroups;
	public final List<SchemaWithMapping> schemas;
	public final List<String> membershipAttributes;
	public final List<String> excludedMembershipGroups;

	private SCIMEndpointConfiguration(Builder builder)
	{
		this.allowedCorsHeaders = List.copyOf(builder.allowedCORSheaders);
		this.allowedCorsOrigins = List.copyOf(builder.allowedCORSorigins);
		this.rootGroup = builder.rootGroup;
		this.restAdminGroup = builder.restAdminGroup;
		this.membershipGroups = List.copyOf(builder.membershipGroups);
		this.excludedMembershipGroups = List.copyOf(builder.excludedMembershipGroups);
		this.schemas = List.copyOf(builder.schemas);
		this.membershipAttributes = List.copyOf(builder.membershipAttributes);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(allowedCorsHeaders, allowedCorsOrigins, membershipGroups, excludedMembershipGroups,
				rootGroup, restAdminGroup, schemas, membershipAttributes);
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
		SCIMEndpointConfiguration other = (SCIMEndpointConfiguration) obj;
		return Objects.equals(allowedCorsHeaders, other.allowedCorsHeaders)
				&& Objects.equals(allowedCorsOrigins, other.allowedCorsOrigins)
				&& Objects.equals(membershipGroups, other.membershipGroups)
				&& Objects.equals(excludedMembershipGroups, other.excludedMembershipGroups)
				&& Objects.equals(membershipAttributes, other.membershipAttributes)
				&& Objects.equals(rootGroup, other.rootGroup) && Objects.equals(restAdminGroup, other.restAdminGroup)
				&& Objects.equals(schemas, other.schemas);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<String> allowedCORSheaders = Collections.emptyList();
		private List<String> allowedCORSorigins = Collections.emptyList();
		private String rootGroup;
		private String restAdminGroup;
		private List<String> membershipGroups = Collections.emptyList();
		private List<String> excludedMembershipGroups = Collections.emptyList();
		private List<String> membershipAttributes = Collections.emptyList();
		private List<SchemaWithMapping> schemas;

		private Builder()
		{
		}

		public Builder withAllowedCorsHeaders(List<String> allowedCorsHeaders)
		{
			this.allowedCORSheaders = allowedCorsHeaders;
			return this;
		}

		public Builder withAllowedCorsOrigins(List<String> allowedCorsOrigins)
		{
			this.allowedCORSorigins = allowedCorsOrigins;
			return this;
		}

		public Builder withRootGroup(String rootGroup)
		{
			this.rootGroup = rootGroup;
			return this;
		}

		public Builder withRestAdminGroup(String adminGroup)
		{
			this.restAdminGroup = adminGroup;
			return this;
		}

		public Builder withMembershipAttributes(List<String> membershipAttributes)
		{
			this.membershipAttributes = membershipAttributes;
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

		public SCIMEndpointConfiguration build()
		{
			return new SCIMEndpointConfiguration(this);
		}
	}
}
