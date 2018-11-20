/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for project management
 * 
 * @author P.Piernik
 *
 */
@Component
public class ProjectController
{
	private UnityMessageSource msg;
	private DelegatedGroupManagement delGroupMan;

	@Autowired
	public ProjectController(UnityMessageSource msg, DelegatedGroupManagement delGroupMan)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
	}

	Map<String, String> getProjectForUser(long entityId) throws ControllerException
	{

		List<Group> projects;
		try
		{
			projects = delGroupMan.getProjectsForEntity(entityId);
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("ProjectController.getProjectsError"), e);
		}

		if (projects.isEmpty())
			throw new ControllerException(
					msg.getMessage("ProjectController.noProjectAvailable"),
					new Throwable());

		Map<String, String> projectMap = new HashMap<>();

		for (Group p : projects)
		{
			String projectName = p.getDisplayedName().getValue(msg);
			if (projectName.equals(p.getName()))
			{
				projectName = p.getNameShort();

			}
			projectMap.put(p.getName(), projectName);
		}

		return projectMap;
	}
}
