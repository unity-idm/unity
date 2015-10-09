/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.Date;

/**
 * Stores information about entity's membership in a group. Group information is not stored here, besides its path. 
 * 
 * @author K. Benedyczak
 */
public class GroupMembership 
{
	private String group;
	private long entityId;
	private Date creationTs;
	private String translationProfile;
	private String remoteIdp;

	public GroupMembership(String group, long entityId, Date creationTs,
			String translationProfile, String remoteIdp)
	{
		this.group = group;
		this.entityId = entityId;
		this.creationTs = creationTs;
		this.translationProfile = translationProfile;
		this.remoteIdp = remoteIdp;
	}

	public GroupMembership(String group, long entityId, Date creationTs)
	{
		this(group, entityId, creationTs, null, null);
	}

	public String getGroup()
	{
		return group;
	}
	public long getEntityId()
	{
		return entityId;
	}
	public Date getCreationTs()
	{
		return creationTs;
	}
	public String getTranslationProfile()
	{
		return translationProfile;
	}
	public String getRemoteIdp()
	{
		return remoteIdp;
	}

	public void setGroup(String group)
	{
		this.group = group;
	}

	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}

	public void setCreationTs(Date creationTs)
	{
		this.creationTs = creationTs;
	}

	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public void setRemoteIdp(String remoteIdp)
	{
		this.remoteIdp = remoteIdp;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (entityId ^ (entityId >>> 32));
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		return result;
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
		GroupMembership other = (GroupMembership) obj;
		if (entityId != other.entityId)
			return false;
		if (group == null)
		{
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "GroupMembership [group=" + group + ", entityId=" + entityId + "]";
	}
}
