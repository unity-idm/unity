/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class SCIMEndpointDescription
{
	public final URI baseLocation;
	public final String rootGroup;
	public final List<String> membershipGroups;
	public final List<String> excludedMembershipGroups;
	public final List<SchemaWithMapping> schemas;
	public final List<String> membershipAttributes;
	public final List<String> authenticationOptions;

	public SCIMEndpointDescription(URI baseLocation, String rootGroup, List<String> membershipGroups, List<String> excludedMembershipGroups,
			List<SchemaWithMapping> schemas, List<String> membershipAttributes, 
			List<String> authenticationOptions)
	{
		this.baseLocation = baseLocation;
		this.rootGroup = rootGroup;
		this.membershipGroups = List.copyOf(membershipGroups);
		this.excludedMembershipGroups = List.copyOf(excludedMembershipGroups);
		this.schemas = List.copyOf(schemas);
		this.membershipAttributes = List.copyOf(membershipAttributes);
		this.authenticationOptions = List.copyOf(authenticationOptions);
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

}
