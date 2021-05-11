/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityWithAttributes
{

	public final Entity entity;
	public final Map<String, List<ExternalizedAttribute>> attributesInGroups;

	public EntityWithAttributes(@JsonProperty("entity") Entity entity,
			@JsonProperty("attributesInGroups") Map<String, List<ExternalizedAttribute>> attributesInGroups)
	{
		this.entity = entity;
		this.attributesInGroups = attributesInGroups;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributesInGroups, entity);
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
				&& Objects.equals(entity, other.entity);
	}
}
