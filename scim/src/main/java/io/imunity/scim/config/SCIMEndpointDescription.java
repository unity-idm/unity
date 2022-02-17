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
	public final List<SchemaWithMapping> schemas;

	public SCIMEndpointDescription(URI baseLocation, String rootGroup, List<String> membershipGroups,
			List<SchemaWithMapping> schemas)
	{
		this.baseLocation = baseLocation;
		this.rootGroup = rootGroup;
		this.membershipGroups = List.copyOf(membershipGroups);
		this.schemas = List.copyOf(schemas);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(baseLocation, membershipGroups, rootGroup, schemas);
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
				&& Objects.equals(membershipGroups, other.membershipGroups)
				&& Objects.equals(rootGroup, other.rootGroup) && Objects.equals(schemas, other.schemas);
	}

}
