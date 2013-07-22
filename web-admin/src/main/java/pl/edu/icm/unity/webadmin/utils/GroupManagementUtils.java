/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.ErrorPopup;

public class GroupManagementUtils
{
	/**
	 * Adds to groups from a deque.
	 * @param notMember
	 * @param entityId
	 * @param msg
	 * @param groupsMan
	 */
	public static void addToGroup(Deque<String> notMember, long entityId, UnityMessageSource msg,
			GroupsManagement groupsMan)
	{
		EntityParam entityParam = new EntityParam(entityId);
		while (!notMember.isEmpty())
		{
			String currentGroup = notMember.pollLast();
			try
			{
				groupsMan.addMemberFromParent(currentGroup, entityParam);
			} catch (Exception e)
			{
				ErrorPopup.showError(msg.getMessage("GroupsTree.addToGroupError", 
						entityId, currentGroup), e);
				break;
			}
		}
	}
	
	/**
	 * Computes deque of full group names which are not in the collection of existingGroups
	 * and are on the path to the finalGroup (inclusive). 
	 * @param finalGroup
	 * @param existingGroups
	 * @return
	 */
	public static Deque<String> getMissingGroups(String finalGroup, Collection<String> existingGroups)
	{
		Group group = new Group(finalGroup);
		String[] path = group.getPath();
		final Deque<String> notMember = new ArrayDeque<String>(path.length);
		for (int i=path.length-1; i>=0 && !existingGroups.contains(group.toString()); i--)
		{
			notMember.addLast(group.toString());
			if (!group.isTopLevel())
				group = new Group(group.getParentPath());
		}
		return notMember;
	}
}
