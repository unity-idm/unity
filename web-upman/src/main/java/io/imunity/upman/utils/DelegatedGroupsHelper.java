/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.utils;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author P.Piernik
 *
 */

@Component
public class DelegatedGroupsHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, DelegatedGroupsHelper.class);
	private DelegatedGroupManagement delGroupMan;
	private MessageSource msg;

	public DelegatedGroupsHelper(DelegatedGroupManagement delGroupMan, MessageSource msg)
	{
		this.delGroupMan = delGroupMan;
		this.msg = msg;
	}

	public List<DelegatedGroup> getProjectGroups(String projectPath) throws EngineException
	{
		return delGroupMan.getGroupAndSubgroups(projectPath, projectPath).values().stream().map(dg -> dg.group).collect(Collectors.toList());
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
				groups.add(con.group.displayedName.getValue(msg));
			} catch (IllegalArgumentException e)
			{
				groups.add(msg.getMessage("DelegatedGroupsHelper.removedGroup"));
			} catch (Exception e)
			{
				log.error("Can not get delegated group displayed name", e);
			}
		}
		return groups;
	}
}
