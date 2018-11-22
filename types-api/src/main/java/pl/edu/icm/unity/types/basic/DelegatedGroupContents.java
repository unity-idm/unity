/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Holds information about delegatedgroup contents.
 * @author P.Piernik
 *
 */
@JsonInclude(Include.NON_NULL)
public class DelegatedGroupContents
{
	private Group group;
	private List<String> subGroups;
	private List<DelegatedGroupMember> members;

	public void setSubGroups(List<String> subGroups)
	{
		this.subGroups = subGroups;
	}

	public void setMembers(List<DelegatedGroupMember> members)
	{
		this.members = members;
	}

	public List<String> getSubGroups()
	{
		return subGroups;
	}

	public List<DelegatedGroupMember> getMembers()
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
