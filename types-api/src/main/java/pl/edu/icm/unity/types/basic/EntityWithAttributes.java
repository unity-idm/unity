/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityWithAttributes
{

	public final Entity entity;
	public final Map<String, GroupMembership> groups;
	public final Map<String, List<ExternalizedAttribute>> attributesInGroups;

	public EntityWithAttributes(@JsonProperty("entity") Entity entity,
			@JsonProperty("groups") Map<String, GroupMembership> groups,
			@JsonProperty("attributesInGroups") Map<String, List<ExternalizedAttribute>> attributesInGroups)
	{
		this.entity = entity;
		this.attributesInGroups = Collections
				.unmodifiableMap(attributesInGroups != null ? attributesInGroups : new HashMap<>());
		this.groups = Collections.unmodifiableMap(groups != null ? groups : new HashMap<>());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributesInGroups, groups, entity);
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
		EntityWithAttributes other = (EntityWithAttributes) obj;
		return Objects.equals(attributesInGroups, other.attributesInGroups)
				&& Objects.equals(groups, other.groups) && Objects.equals(entity, other.entity);
	}
}
