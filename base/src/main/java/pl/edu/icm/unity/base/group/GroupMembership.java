/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.group;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;

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
	
	public GroupMembership(GroupMembership src)
	{
		this.group = src.group;
		this.entityId = src.entityId;
		this.creationTs = src.creationTs;
		this.translationProfile = src.translationProfile;
		this.remoteIdp = src.remoteIdp;
	}
	
	@JsonCreator
	public GroupMembership(ObjectNode src)
	{
		fromJson(src);
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

	private void fromJson(ObjectNode src)
	{
		setGroup(src.get("group").asText());
		setEntityId(src.get("entityId").asLong());
		fromJsonBase(src);
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = toJsonBase();
		main.put("group", getGroup());
		main.put("entityId", getEntityId());
		return main;
	}
	
	public ObjectNode toJsonBase()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		if (getRemoteIdp() != null)
			main.put("remoteIdp", getRemoteIdp());
		if (getTranslationProfile() != null)
			main.put("translationProfile", getTranslationProfile());
		if (getCreationTs() != null)
			main.put("creationTs", getCreationTs().getTime());
		return main;
	}
	
	public void fromJsonBase(ObjectNode main)
	{
		if (main.has("remoteIdp"))
			setRemoteIdp(main.get("remoteIdp").asText());
		if (main.has("translationProfile"))
			setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("creationTs"))
			setCreationTs(new Date(main.get("creationTs").asLong()));		
	}
	
	@Override
	public String toString()
	{
		return "GroupMembership [group=" + group + ", entityId=" + entityId + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationTs == null) ? 0 : creationTs.hashCode());
		result = prime * result + (int) (entityId ^ (entityId >>> 32));
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((remoteIdp == null) ? 0 : remoteIdp.hashCode());
		result = prime * result + ((translationProfile == null) ? 0
				: translationProfile.hashCode());
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
		if (creationTs == null)
		{
			if (other.creationTs != null)
				return false;
		} else if (!creationTs.equals(other.creationTs))
			return false;
		if (entityId != other.entityId)
			return false;
		if (group == null)
		{
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (remoteIdp == null)
		{
			if (other.remoteIdp != null)
				return false;
		} else if (!remoteIdp.equals(other.remoteIdp))
			return false;
		if (translationProfile == null)
		{
			if (other.translationProfile != null)
				return false;
		} else if (!translationProfile.equals(other.translationProfile))
			return false;
		return true;
	}
}
