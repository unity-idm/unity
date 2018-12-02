/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Helper for creating a map with groups which names are indented
 * @author P.Piernik
 *
 */
public class GroupIndentHelper
{
	public static final String GROUPS_TREE_INDENT_CHAR = "\u2003";
	
	/**
	 * 
	 * @param projectPath
	 * @param groupAndSubgroups
	 * @return keys of the returned map are group paths. Values are indented group names 
	 * @throws ControllerException
	 */
	public static Map<String, String> getProjectIndentGroupsMap(String projectPath,
			Map<String, DelegatedGroupContents> groupAndSubgroups) throws ControllerException
	{
		Map<String, String> groups = new HashMap<>();
		groups.put(projectPath, groupAndSubgroups.get(projectPath).group.displayedName);

		fillGroupRecursive(projectPath, groupAndSubgroups, groups);

		return getGroupTree(projectPath, groups);
	}

	private static Map<String, String> getGroupTree(String rootPath, Map<String, String> groups)
	{
		Map<String, String> tree = new HashMap<>();
		int initIndend = StringUtils.countOccurrencesOf(rootPath, "/");

		tree.put(rootPath, groups.get(rootPath));
		for (String gr : groups.keySet().stream().filter(i -> !i.equals(rootPath)).collect(Collectors.toList()))
		{
			tree.put(gr, generateIndent(StringUtils.countOccurrencesOf(gr, "/") - initIndend)
					+ groups.get(gr));
		}
		return tree;

	}

	private static String generateIndent(int count)
	{
		return String.join("", Collections.nCopies(count, GROUPS_TREE_INDENT_CHAR));
	}

	private static void fillGroupRecursive(String parentPath, Map<String, DelegatedGroupContents> groupAndSubgroups,
			Map<String, String> groups)
	{
		for (String subgroup : groupAndSubgroups.get(parentPath).subGroups)
		{
			groups.put(subgroup, groupAndSubgroups.get(subgroup).group.displayedName);
			fillGroupRecursive(subgroup, groupAndSubgroups, groups);
		}
	}
}
