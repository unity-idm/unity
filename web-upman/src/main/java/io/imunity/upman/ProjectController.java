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

import com.vaadin.server.Resource;

import pl.edu.icm.unity.engine.api.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.delegatedgroup.DelegatedGroup;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
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

		List<DelegatedGroup> projects;
		try
		{
			projects = delGroupMan.getProjectsForEntity(entityId);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("ProjectController.getProjectsError"), e);
		}

		if (projects.isEmpty())
			throw new ControllerException(
					msg.getMessage("ProjectController.noProjectAvailable"),
					new Throwable());

		Map<String, String> projectMap = new HashMap<>();

		for (DelegatedGroup p : projects)
		{
			projectMap.put(p.path, p.displayedName);
		}

		return projectMap;
	}

	public Resource getProjectLogoSafe(String projectPath)
	{
		Resource logo = Images.logoSmall.getResource();
		DelegatedGroup group;
		try
		{
			group = delGroupMan.getContents(projectPath, projectPath).group;
		} catch (Exception e)
		{
			return logo;
		}
		GroupDelegationConfiguration config = group.delegationConfiguration;
		String logoUrl = config.getLogoUrl();
		if (logoUrl != null && !logoUrl.isEmpty())
		{
			return ImageUtils.getConfiguredImageResource(logoUrl);
		}

		return logo;
	}
}
