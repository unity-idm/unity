/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.components;

import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.webui.common.Images;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ProjectService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, ProjectService.class);

	private final MessageSource msg;
	private final DelegatedGroupManagement delGroupMan;
	private final Vaddin23WebLogoutHandler logoutHandler;

	public ProjectService(MessageSource msg, DelegatedGroupManagement delGroupMan, Vaddin23WebLogoutHandler logoutHandler)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.logoutHandler = logoutHandler;
	}

	public List<ProjectGroup> getProjectForUser(long entityId)
	{
		List<DelegatedGroup> projects;
		try
		{
			projects = delGroupMan.getProjectsForEntity(entityId);
		} catch (Exception e)
		{
			log.warn("Can not get projects for entity " + entityId, e);
			NotificationPresenter.showCriticalError(logoutHandler, msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			return List.of();
		}

		if (projects.isEmpty())
		{
			NotificationPresenter.showCriticalError(logoutHandler, msg.getMessage("ProjectController.noProjectAvailable"), null);
			return List.of();
		}

		return projects.stream()
				.map(group -> new ProjectGroup(group.path, group.displayedName))
				.collect(Collectors.toList());
	}

	public String getProjectLogo(ProjectGroup projectGroup)
	{
		DelegatedGroup group;
		try
		{
			group = delGroupMan.getContents(projectGroup.path, projectGroup.path).group;
		} catch (Exception e)
		{
			return Images.logoSmall.getPath();
		}
		return group.delegationConfiguration.logoUrl;
	}

	public Group getProjectGroup(ProjectGroup projectGroup)
	{
		try
		{
			DelegatedGroup group = delGroupMan.getContents(projectGroup.path, projectGroup.path).group;
			return new Group(group.path, group.displayedName, group.delegationConfiguration.enabled, 0);
		}
		catch (Exception e)

		{
			log.warn("Can not get project group " + projectGroup.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			throw new IllegalStateException(e);
		}
	}

	public GroupAuthorizationRole getCurrentUserProjectRole(ProjectGroup projectGroup)
	{
		try
		{

			return delGroupMan.getGroupAuthorizationRole(projectGroup.path,
					InvocationContext.getCurrent().getLoginSession().getEntityId());

		} catch (Exception e)
		{
			log.warn("Can not get project authorization role " + projectGroup.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			throw new IllegalStateException(e);
		}
	}
}
