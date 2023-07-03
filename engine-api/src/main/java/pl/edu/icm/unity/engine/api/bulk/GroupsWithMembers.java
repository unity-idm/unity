/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulk;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.base.entity.Entity;

public class GroupsWithMembers
{
	public final Map<Long, Entity> entities;
	public final Map<String, List<EntityGroupAttributes>> membersByGroup;
	
	public GroupsWithMembers(Map<Long, Entity> entities, Map<String, List<EntityGroupAttributes>> members)
	{
		this.entities = Collections.unmodifiableMap(entities);
		this.membersByGroup = Collections.unmodifiableMap(members);
	}
}
