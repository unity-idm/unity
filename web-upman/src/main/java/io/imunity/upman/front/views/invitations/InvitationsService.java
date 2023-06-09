/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import io.imunity.vaadin23.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement.AlreadyMemberException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class InvitationsService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, InvitationsService.class);

	private final ProjectInvitationsManagement invitationMan;
	private final DelegatedGroupsHelper delGroupHelper;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	public InvitationsService(MessageSource msg, ProjectInvitationsManagement invitationMan,
	                          DelegatedGroupsHelper delGroupHelper, NotificationPresenter notificationPresenter)
	{
		this.invitationMan = invitationMan;
		this.delGroupHelper = delGroupHelper;
		this.notificationPresenter = notificationPresenter;
		this.msg = msg;
	}

	public void resendInvitations(ProjectGroup projectGroup, Set<InvitationModel> items)
	{
		List<String> sent = new ArrayList<>();
		try {
			for (InvitationModel inv : items)
			{
				invitationMan.sendInvitation(projectGroup.path, inv.code);
				sent.add(inv.email);
			}
			notificationPresenter.showSuccess(msg.getMessage("InvitationsComponent.sent"));
		} catch (Exception e)
		{
			log.warn("Can not resend invitations", e);
			if (sent.isEmpty())
			{
				notificationPresenter.showError(
						msg.getMessage("InvitationsController.resendInvitationError"),
						msg.getMessage("InvitationsController.notSend")
				);
			} else {
				notificationPresenter.showError(
						msg.getMessage("InvitationsController.resendInvitationError"),
						msg.getMessage("InvitationsController.partiallySend", sent)
				);
			}
		}
	}

	public void removeInvitations(ProjectGroup projectGroup, Set<InvitationModel> items)
	{
		List<String> removed = new ArrayList<>();
		try {
			for (InvitationModel inv : items)
			{
				invitationMan.removeInvitation(projectGroup.path, inv.code);
				removed.add(inv.email);
			}
			notificationPresenter.showSuccess(msg.getMessage("InvitationsComponent.removed"));
		} catch (Exception e)
		{
			log.warn("Can not remove invitations", e);
			if (removed.isEmpty())
			{
				notificationPresenter.showError(
						msg.getMessage("InvitationsController.removeInvitationError"),
						msg.getMessage("InvitationsController.notRemoved")
				);
			} else {
				notificationPresenter.showError(
						msg.getMessage("InvitationsController.removeInvitationError"),
						msg.getMessage("InvitationsController.partiallyRemoved", removed)
				);
			}
		}

	}

	public List<InvitationModel> getInvitations(ProjectGroup projectGroup)
	{
		List<ProjectInvitation> invitations;
		try {
			invitations = invitationMan.getInvitations(projectGroup.path);
		} catch (Exception e)
		{
			log.warn("Can not get project invitations", e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			return List.of();
		}

		return invitations.stream()
				.map(pinv -> new InvitationModel(pinv.registrationCode, pinv.contactAddress,
						delGroupHelper.getGroupsDisplayedNames(projectGroup.path, pinv.groups),
						pinv.lastSentTime, pinv.expiration, pinv.link))
				.collect(Collectors.toList());
	}

	public void addInvitations(InvitationRequest invitationRequest)
	{
		List<String> added = new ArrayList<>();
		List<String> alredyMember = new ArrayList<>();

		List<String> groups = invitationRequest.groups.stream()
				.map(GroupTreeNode::getPath)
				.collect(Collectors.toList());
		for (String email : invitationRequest.emails)
		{
			try 
			{
				ProjectInvitationParam projectInvitationParam = new ProjectInvitationParam(
						invitationRequest.projectGroup.path, 
						email, 
						groups, 
						invitationRequest.allowModifyGroups, 
						invitationRequest.expiration);
				invitationMan.addInvitation(projectInvitationParam);
				added.add(projectInvitationParam.contactAddress);

			} catch (AlreadyMemberException e)
			{
				alredyMember.add(email);
			} catch (Exception e)
			{
				log.warn("Can not add invitations", e);
				if (added.isEmpty())
				{
					notificationPresenter.showError(
							msg.getMessage("InvitationsController.addInvitationError"),
							msg.getMessage("InvitationsController.notAdd")
					);
				} else {
					notificationPresenter.showError(
							msg.getMessage("InvitationsController.addInvitationError"),
							msg.getMessage("InvitationsController.partiallyAdded", String.join(",", added))
					);
				}
			}
		}
		if (!alredyMember.isEmpty())
		{
			notificationPresenter.showWarning(
					msg.getMessage("InvitationsController.alreadyAMember", String.join(",", alredyMember)),
					""
			);
		}

	}
}
