/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.components;

import io.imunity.upman.common.ServerFaultException;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class ProjectController23
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, ProjectController23.class);

	private MessageSource msg;
	private DelegatedGroupManagement delGroupMan;
	private ImageAccessService imageAccessService;

	@Autowired
	public ProjectController23(MessageSource msg, DelegatedGroupManagement delGroupMan, ImageAccessService imageAccessService)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.imageAccessService = imageAccessService;
	}

	public Map<String, String> getProjectForUser(long entityId) throws ControllerException
	{

		List<DelegatedGroup> projects;
		try
		{
			projects = delGroupMan.getProjectsForEntity(entityId);
		} catch (Exception e)
		{
			log.warn("Can not get projects for entity " + entityId, e);
			throw new ServerFaultException(msg);
		}

		if (projects.isEmpty())
			throw new ControllerException(
					msg.getMessage("ProjectController.noProjectAvailable"),
					null);

		return projects.stream().collect(Collectors.toMap(p -> p.path, p -> p.displayedName));
	}

	public String getProjectLogo(String projectPath)
	{
		DelegatedGroup group;
		try
		{
			group = delGroupMan.getContents(projectPath, projectPath).group;
		} catch (Exception e)
		{
			return Images.logoSmall.getPath();
		}
		return group.delegationConfiguration.logoUrl;
	}
}
