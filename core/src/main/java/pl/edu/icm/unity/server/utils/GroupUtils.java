/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import pl.edu.icm.unity.types.basic.Group;

/**
 * Group related utilities.
 * @author K. Benedyczak
 */
public class GroupUtils
{
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
		final Deque<String> notMember = new ArrayDeque<>(path.length);
		for (int i=path.length-1; i>=0 && !existingGroups.contains(group.toString()); i--)
		{
			notMember.addLast(group.toString());
			if (!group.isTopLevel())
				group = new Group(group.getParentPath());
		}
		return notMember;
	}
}







