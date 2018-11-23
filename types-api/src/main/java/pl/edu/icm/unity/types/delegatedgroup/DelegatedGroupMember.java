/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.delegatedgroup;

import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Contains information about delegated group member. 
 * @author P.Piernik
 *
 */
public class DelegatedGroupMember
{
	public final long entityId;
	public final String project;
	public final String group;
	public final GroupAuthorizationRole role;
	public final String name;
	public final String email;
	public final List<Attribute> attributes;

	public DelegatedGroupMember(long entityId, String project, String group,
			GroupAuthorizationRole role, String name,
			String email, List<Attribute> attributes)
	{
		this.entityId = entityId;
		this.project = project;
		this.group = group;
		this.role = role;
		this.name = name;
		this.email = email;
		this.attributes = attributes;
	}
}
