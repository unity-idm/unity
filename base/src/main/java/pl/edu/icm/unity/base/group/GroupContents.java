/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.group;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Holds information about group contents.
 * @author K. Benedyczak
 */
@JsonInclude(Include.NON_NULL)
public class GroupContents
{
	public static final int GROUPS = 0x01;
	public static final int MEMBERS = 0x04;
	public static final int METADATA = 0x08;
	public static final int EVERYTHING = GROUPS | MEMBERS | METADATA;
	
	private Group group;
	private List<String> subGroups;
	private List<GroupMembership> members;
	
	public void setSubGroups(List<String> subGroups)
	{
		this.subGroups = subGroups;
	}
	public void setMembers(List<GroupMembership> members)
	{
		this.members = members;
	}
	public List<String> getSubGroups()
	{
		return subGroups;
	}
	public List<GroupMembership> getMembers()
	{
		return members;
	}
	public Group getGroup()
	{
		return group;
	}
	public void setGroup(Group group)
	{
		this.group = group;
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(group, members, subGroups);
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
		GroupContents other = (GroupContents) obj;
		return Objects.equals(group, other.group) && Objects.equals(members, other.members)
				&& Objects.equals(subGroups, other.subGroups);
	}
	
	
}
