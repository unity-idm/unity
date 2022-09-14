/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.utils;

import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.endpoint.common.Vaddin23WebLogoutHandler;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.webui.common.Images;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ProjectService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, ProjectService.class);

	private final MessageSource msg;
	private final DelegatedGroupManagement delGroupMan;
	private final DelegatedGroupsHelper delGroupHelper;
	private final Vaddin23WebLogoutHandler logoutHandler;
	private final NotificationPresenter notificationPresenter;

	public ProjectService(MessageSource msg, DelegatedGroupManagement delGroupMan, Vaddin23WebLogoutHandler logoutHandler,
	                      DelegatedGroupsHelper delGroupHelper, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.delGroupHelper = delGroupHelper;
		this.logoutHandler = logoutHandler;
		this.notificationPresenter = notificationPresenter;
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
			NotificationPresenter.showCriticalError(logoutHandler::logout, msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			return List.of();
		}

		if (projects.isEmpty())
		{
			NotificationPresenter.showCriticalError(logoutHandler::logout, msg.getMessage("ProjectController.noProjectAvailable"), null);
			return List.of();
		}

		return projects.stream()
				.map(group -> new ProjectGroup(group.path, group.displayedName.getValue(msg), group.delegationConfiguration.registrationForm, group.delegationConfiguration.signupEnquiryForm))
				.collect(Collectors.toList());
	}

	public GroupTreeNode getProjectGroups(ProjectGroup projectGroup)
	{
		GroupTreeNode groupTreeNode = new GroupTreeNode(getProjectGroup(projectGroup), 0);
		try
		{
			delGroupHelper.getProjectGroups(projectGroup.path)
					.stream()
					.sorted(Comparator.comparing(group -> group.path))
					.map(this::createGroup)
					.forEach(groupTreeNode::addChild);
			return groupTreeNode;
		} catch (Exception e)
		{
			log.warn("Can not get group " + projectGroup.path, e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
		return groupTreeNode;
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
			return createGroup(group);
		}
		catch (Exception e)

		{
			log.warn("Can not get project group " + projectGroup.path, e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			throw new IllegalStateException(e);
		}
	}

	private Group createGroup(DelegatedGroup group)
	{
		return new Group(group.path, group.displayedName, group.displayedName.getValue(msg), group.delegationConfiguration.enabled, group.delegationConfiguration.enableSubprojects, group.delegationConfiguration.logoUrl, group.open, 0);
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
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			throw new IllegalStateException(e);
		}
	}
}
