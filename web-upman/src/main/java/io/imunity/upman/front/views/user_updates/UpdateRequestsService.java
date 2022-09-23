/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.user_updates;

import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import io.imunity.vaadin23.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.exceptions.EngineException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class UpdateRequestsService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, UpdateRequestsService.class);

	private final ProjectRequestManagement requestMan;
	private final DelegatedGroupsHelper delGroupHelper;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;


	public UpdateRequestsService(MessageSource msg, ProjectRequestManagement requestMan,
	                             DelegatedGroupsHelper delGroupHelper, NotificationPresenter notificationPresenter)
	{
		this.requestMan = requestMan;
		this.delGroupHelper = delGroupHelper;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	public Optional<String> getProjectRegistrationFormLink(ProjectGroup projectGroup)
	{
		try
		{
			return requestMan.getProjectRegistrationFormLink(projectGroup.path);
		} catch (EngineException e)
		{
			log.warn("Can not get project registration form link " + projectGroup.path, e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
		return Optional.empty();
	}

	public Optional<String> getProjectSingUpEnquiryFormLink(ProjectGroup projectGroup)
	{
		try
		{
			return requestMan.getProjectSignUpEnquiryFormLink(projectGroup.path);
		} catch (EngineException e)
		{
			log.warn("Can not get project signup enquiry form link " + projectGroup.path, e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
		return Optional.empty();
	}
	
	public Optional<String> getProjectUpdateMembershipEnquiryFormLink(ProjectGroup projectGroup)
	{
		try
		{
			return requestMan.getProjectUpdateMembershipEnquiryFormLink(projectGroup.path);
		} catch (EngineException e)
		{
			log.warn("Can not get project signup enquiry form link " + projectGroup.path, e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
		return Optional.empty();
	}

	public List<UpdateRequestModel> getUpdateRequests(ProjectGroup projectGroup)
	{
		List<ProjectRequest> requests;
		try
		{
			requests = requestMan.getRequests(projectGroup.path);
			return requests.stream()
					.map(projectRequest -> UpdateRequestModel.builder()
							.id(projectRequest.id)
							.operation(projectRequest.operation)
							.name(projectRequest.name)
							.type(projectRequest.type)
							.email(projectRequest.email)
							.requestedTime(projectRequest.requestedTime)
							.groupsDisplayedNames(delGroupHelper.getGroupsDisplayedNames(projectGroup.path, projectRequest.groups))
							.build())
					.collect(Collectors.toList());
		} catch (EngineException e)
		{
			log.warn("Can not get request of group " + projectGroup, e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
		return List.of();
	}

	public void accept(ProjectGroup projectGroup, Set<UpdateRequestModel> items)
	{
		List<String> accepted = new ArrayList<>();
		try
		{
			for (UpdateRequestModel request : items)
			{
				requestMan.accept(new ProjectRequestParam(projectGroup.path, request.id, request.operation, request.type));
				accepted.add(request.email.value);

			}
			notificationPresenter.showSuccess(msg.getMessage("UpdateRequestsComponent.accepted"));
		} catch (Exception e)
		{
			log.warn("Can not accept request ", e);
			if (accepted.isEmpty())
			{
				notificationPresenter.showError(
						msg.getMessage("UpdateRequestsController.acceptRequestError"),
						msg.getMessage("UpdateRequestsController.notAccepted")
				);
			} else
			{
				notificationPresenter.showError(
						msg.getMessage("UpdateRequestsController.removeFromGroupError"),
						msg.getMessage("UpdateRequestsController.partiallyAccepted", accepted)
				);
			}
		}
	}

	public void decline(ProjectGroup projectGroup, Set<UpdateRequestModel> items)
	{
		List<String> declined = new ArrayList<>();
		try
		{
			for (UpdateRequestModel request : items)
			{
				requestMan.decline(new ProjectRequestParam(projectGroup.path, request.id, request.operation, request.type));
				declined.add(request.email.value);

			}
			notificationPresenter.showSuccess(msg.getMessage("UpdateRequestsComponent.declined"));
		} catch (Exception e)
		{
			log.warn("Can not reject request ", e);
			if (declined.isEmpty())
			{
				notificationPresenter.showError(
						msg.getMessage("UpdateRequestsController.declineRequestError"),
						msg.getMessage("UpdateRequestsController.notDeclined")
				);
			} else
			{
				notificationPresenter.showError(
						msg.getMessage("UpdateRequestsController.declineRequestError"),
						msg.getMessage("UpdateRequestsController.partiallyDeclined", declined)
				);
			}
		}
	}

}
