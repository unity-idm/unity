/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.sql.Timestamp;

/**
 * In DB representation of the "attributes update pending" flag of an entity in a group.
 */
public class AttributesCachePendingBean
{
	private Long entityId;
	private Long groupId;
	private Timestamp created;

	private String group;

	public AttributesCachePendingBean()
	{
	}

	public AttributesCachePendingBean(long entityId, long groupId)
	{
		this.entityId = entityId;
		this.groupId = groupId;
	}

	public AttributesCachePendingBean(long entityId, long groupId, Timestamp created)
	{
		this.entityId = entityId;
		this.groupId = groupId;
		this.created = created;
	}

	public Long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(Long entityId)
	{
		this.entityId = entityId;
	}
	public Long getGroupId()
	{
		return groupId;
	}
	public void setGroupId(Long groupId)
	{
		this.groupId = groupId;
	}
	public Timestamp getCreated()
	{
		return created;
	}
	public void setCreated(Timestamp created)
	{
		this.created = created;
	}
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
}
