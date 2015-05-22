/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

/**
 * Change of group membership
 * @author K. Benedyczak
 */
public class GroupElementChangeBean extends GroupElementBean
{
	private long newEntityId;

	public GroupElementChangeBean(long groupId, long elementId, long newEntityId)
	{
		super(groupId, elementId);
		this.newEntityId = newEntityId;
	}

	public long getNewEntityId()
	{
		return newEntityId;
	}

	public void setNewEntityId(long newEntityId)
	{
		this.newEntityId = newEntityId;
	}
}
