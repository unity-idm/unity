/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.entity;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import pl.edu.icm.unity.types.basic.Entity;

public class EntityWithContactInfo
{
	public final Entity entity;
	public final String contactEmail;
	public final Set<String> groups;

	public EntityWithContactInfo(Entity entity, String contactEmail, Set<String> groups)
	{
		this.entity = entity;
		this.contactEmail = contactEmail;
		this.groups = Optional.ofNullable(groups).map(Set::copyOf).orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(contactEmail, entity, groups);
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
		EntityWithContactInfo other = (EntityWithContactInfo) obj;
		return Objects.equals(contactEmail, other.contactEmail) && Objects.equals(entity, other.entity)
				&& Objects.equals(groups, other.groups);
	}

}
