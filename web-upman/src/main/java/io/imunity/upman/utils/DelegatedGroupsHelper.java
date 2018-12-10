/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * @author P.Piernik
 *
 */

@Component
public class DelegatedGroupsHelper
{
	public static final String GROUPS_TREE_INDENT_CHAR = "\u2003";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER, DelegatedGroupsHelper.class);
	private DelegatedGroupManagement delGroupMan;

	public DelegatedGroupsHelper(DelegatedGroupManagement delGroupMan)
	{
		this.delGroupMan = delGroupMan;
	}

	public List<String> getGroupsDisplayedNames(String project, List<String> groupPaths)
	{
		List<String> groups = new ArrayList<>();
		for (String path : groupPaths)
		{
			DelegatedGroupContents con;
			try
			{
				con = delGroupMan.getContents(project, path);
				groups.add(con.group.displayedName);
			} catch (EngineException e)
			{
				log.debug("Can not get delegated group displayed name", e);
			}
		}
		return groups;
	}

	public Map<String, String> getProjectIndentGroupsMap(String projectPath) throws EngineException
	{
		Map<String, DelegatedGroupContents> groupAndSubgroups;
		groupAndSubgroups = delGroupMan.getGroupAndSubgroups(projectPath, projectPath);
		return getProjectIndentGroupsMap(projectPath, groupAndSubgroups);
	}

	private Map<String, String> getProjectIndentGroupsMap(String projectPath,
			Map<String, DelegatedGroupContents> groupAndSubgroups)
	{
		Map<String, String> groups = new HashMap<>();
		groups.put(projectPath, groupAndSubgroups.get(projectPath).group.displayedName);

		fillGroupRecursive(projectPath, groupAndSubgroups, groups);

		return getGroupTree(projectPath, groups);
	}

	private Map<String, String> getGroupTree(String rootPath, Map<String, String> groups)
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

	private String generateIndent(int count)
	{
		return String.join("", Collections.nCopies(count, GROUPS_TREE_INDENT_CHAR));
	}

	private void fillGroupRecursive(String parentPath, Map<String, DelegatedGroupContents> groupAndSubgroups,
			Map<String, String> groups)
	{
		for (String subgroup : groupAndSubgroups.get(parentPath).subGroups)
		{
			groups.put(subgroup, groupAndSubgroups.get(subgroup).group.displayedName);
			fillGroupRecursive(subgroup, groupAndSubgroups, groups);
		}
	}
}
