/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.upman.common.ServerFaultException;
import io.imunity.upman.utils.GroupIndentHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Invitations controller
 * 
 * @author P.Piernik
 *
 */
@Component
public class InvitationsController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, InvitationsController.class);

	private ProjectInvitationsManagement invitationMan;
	private DelegatedGroupManagement delGroupMan;
	private UnityMessageSource msg;

	@Autowired
	public InvitationsController(UnityMessageSource msg, ProjectInvitationsManagement invitationMan,
			DelegatedGroupManagement delGroupMan)
	{
		this.invitationMan = invitationMan;
		this.delGroupMan = delGroupMan;
		this.msg = msg;
	}

	public void resendInvitations(String projectPath, Set<InvitationEntry> items) throws ControllerException
	{
		List<String> sent = new ArrayList<>();
		try
		{
			for (InvitationEntry inv : items)
			{
				invitationMan.sendInvitation(projectPath, inv.code);
				sent.add(inv.email);
			}
		} catch (Exception e)
		{
			log.debug("Can not resend invitations", e);
			if (sent.isEmpty())
			{
				throw new ControllerException(
						msg.getMessage("InvitationsController.resendInvitationError"),
						msg.getMessage("InvitationsController.notSend"), null);
			} else
			{
				throw new ControllerException(
						msg.getMessage("InvitationsController.resendInvitationError"),
						msg.getMessage("InvitationsController.partiallySend", sent), null);
			}
		}
	}

	public void deleteInvitations(String projectPath, Set<InvitationEntry> items) throws ControllerException
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (InvitationEntry inv : items)
			{
				invitationMan.removeInvitation(projectPath, inv.code);
				removed.add(inv.email);
			}
		} catch (Exception e)
		{
			log.debug("Can not remove invitations", e);
			if (removed.isEmpty())
			{
				throw new ControllerException(
						msg.getMessage("InvitationsController.removeInvitationError"),
						msg.getMessage("InvitationsController.notRemoved"), null);
			} else
			{
				throw new ControllerException(
						msg.getMessage("InvitationsController.removeInvitationError"),
						msg.getMessage("InvitationsController.partiallyRemoved", removed),
						null);
			}
		}

	}

	public List<InvitationEntry> getInvitations(String projectPath) throws ControllerException
	{
		List<ProjectInvitation> invitations;
		try
		{
			invitations = invitationMan.getInvitations(projectPath);
		} catch (Exception e)
		{
			throw new ServerFaultException(msg);
		}

		return invitations.stream().map(i -> new InvitationEntry(i)).collect(Collectors.toList());
	}

	public void addInvitation(ProjectInvitationParam invitation) throws ControllerException
	{
		try
		{
			String code = invitationMan.addInvitation(invitation);
			invitationMan.sendInvitation(invitation.getProject(), code);

		} catch (Exception e)
		{
			log.debug("Can not add invitation", e);
			throw new ServerFaultException(msg);
		}
	}

	public Map<String, String> getAllowedIndentGroupsMap(String projectPath) throws ControllerException
	{
		Map<String, DelegatedGroupContents> groupAndSubgroups;
		try
		{
			groupAndSubgroups = delGroupMan.getGroupAndSubgroups(projectPath, projectPath);
		} catch (Exception e)
		{
			log.debug("Can not get group " + projectPath, e);
			throw new ServerFaultException(msg);
		}
		return GroupIndentHelper.getProjectIndentGroupsMap(projectPath, groupAndSubgroups);
	}
}
