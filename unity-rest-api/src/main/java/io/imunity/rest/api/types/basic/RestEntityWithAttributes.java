/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;



public class RestEntityWithAttributes
{
	public final RestEntity entity;
	public final Map<String, RestGroupMembership> groups;
	public final Map<String, List<RestExternalizedAttribute>> attributesInGroups;

	public RestEntityWithAttributes(@JsonProperty("entity") RestEntity entity,
			@JsonProperty("groups") Map<String, RestGroupMembership> groups,
			@JsonProperty("attributesInGroups") Map<String, List<RestExternalizedAttribute>> attributesInGroups)
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
		RestEntityWithAttributes other = (RestEntityWithAttributes) obj;
		return Objects.equals(attributesInGroups, other.attributesInGroups)
				&& Objects.equals(groups, other.groups)
				&& Objects.equals(entity, other.entity);
	}
}
