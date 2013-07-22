/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.List;

/**
 * Holds information about group contents.
 * @author K. Benedyczak
 */
public class GroupContents
{
	public static final int GROUPS = 0x01;
	public static final int LINKED_GROUPS = 0x02;
	public static final int MEMBERS = 0x04;
	public static final int METADATA = 0x08;
	public static final int ALL_GROUPS = GROUPS | LINKED_GROUPS;
	public static final int EVERYTHING = ALL_GROUPS | MEMBERS | METADATA;
	
	private Group group;
	private List<String> subGroups;
	private List<String> linkedGroups;
	private List<Long> members;
	
	public void setSubGroups(List<String> subGroups)
	{
		this.subGroups = subGroups;
	}
	public void setLinkedGroups(List<String> linkedGroups)
	{
		this.linkedGroups = linkedGroups;
	}
	public void setMembers(List<Long> members)
	{
		this.members = members;
	}
	public List<String> getSubGroups()
	{
		return subGroups;
	}
	public List<String> getLinkedGroups()
	{
		return linkedGroups;
	}
	
	/**
	 * @return list of entity ids
	 */
	public List<Long> getMembers()
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
}
