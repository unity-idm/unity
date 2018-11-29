/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.upman.common.ServerFaultException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
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
	private UnityMessageSource msg;

	@Autowired
	public InvitationsController(UnityMessageSource msg, ProjectInvitationsManagement invitationMan)
	{
		this.invitationMan = invitationMan;
		this.msg = msg;
	}

	public void resendInvitations(String projectPath, Set<InvitationEntry> items) throws ControllerException
	{
		for (InvitationEntry inv : items)
		{
			try
			{
				invitationMan.sendInvitation(projectPath, inv.code);
			} catch (Exception e)
			{
				log.debug("Can not send invitation", e);
				// TODO partially exception add
				throw new ServerFaultException(msg);
			}
		}

	}

	public void deleteInvitations(String projectPath, Set<InvitationEntry> items) throws ControllerException
	{
		for (InvitationEntry inv : items)
		{
			try
			{
				invitationMan.removeInvitation(projectPath, inv.code);
			} catch (Exception e)
			{
				log.debug("Can not delete invitation", e);
				// TODO partially exception add
				throw new ServerFaultException(msg);
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
}
