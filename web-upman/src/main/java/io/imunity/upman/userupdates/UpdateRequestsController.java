/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import io.imunity.upman.common.ServerFaultException;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Update request controller
 * 
 * @author P.Piernik
 *
 */
@Component
public class UpdateRequestsController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, UpdateRequestsController.class);

	private ProjectRequestManagement requestMan;
	private DelegatedGroupsHelper delGroupHelper;
	private UnityMessageSource msg;

	public UpdateRequestsController(UnityMessageSource msg, ProjectRequestManagement requestMan,
			DelegatedGroupsHelper delGroupHelper)
	{
		this.requestMan = requestMan;
		this.delGroupHelper = delGroupHelper;
		this.msg = msg;
	}

	public Optional<String> getProjectRegistrationFormLink(String projectPath) throws ControllerException
	{
		try
		{
			return requestMan.getProjectRegistrationFormLink(projectPath);
		} catch (EngineException e)
		{
			log.debug("Can not get project registration form link " + projectPath, e);
			throw new ServerFaultException(msg);
		}

	}

	public Optional<String> getProjectSingUpEnquiryFormLink(String projectPath) throws ControllerException
	{
		try
		{
			return requestMan.getProjectSignUpEnquiryFormLink(projectPath);
		} catch (EngineException e)
		{
			log.debug("Can not get project signup enquiry form link " + projectPath, e);
			throw new ServerFaultException(msg);
		}

	}
	
	public Optional<String> getProjectUpdateMembershipEnquiryFormLink(String projectPath) throws ControllerException
	{
		try
		{
			return requestMan.getProjectUpdateMembershipEnquiryFormLink(projectPath);
		} catch (EngineException e)
		{
			log.debug("Can not get project signup enquiry form link " + projectPath, e);
			throw new ServerFaultException(msg);
		}

	}

	public List<UpdateRequestEntry> getUpdateRequests(String projectPath) throws ControllerException
	{
		List<ProjectRequest> requests;
		try
		{
			requests = requestMan.getRequests(projectPath);
			return requests.stream()
					.map(r -> new UpdateRequestEntry(r.id, r.operation, r.type, r.email, r.name,
							delGroupHelper.getGroupsDisplayedNames(projectPath, r.groups),
							r.requestedTime))
					.collect(Collectors.toList());
		} catch (EngineException e)
		{
			log.debug("Can not get request of group " + projectPath, e);
			throw new ServerFaultException(msg);
		}
	}

	public void accept(String projectPath, Set<UpdateRequestEntry> items) throws ControllerException
	{
		List<String> accepted = new ArrayList<>();
		try
		{
			for (UpdateRequestEntry request : items)
			{
				requestMan.accept(new ProjectRequestParam(projectPath, request.id, request.operation, request.type));
				accepted.add(request.email.getValue());

			}
		} catch (Exception e)
		{
			log.debug("Can not accept request ", e);
			if (accepted.isEmpty())
			{
				throw new ControllerException(
						msg.getMessage("UpdateRequestsController.acceptRequestError"),
						msg.getMessage("UpdateRequestsController.notAccepted"), null);
			} else
			{
				throw new ControllerException(
						msg.getMessage("UpdateRequestsController.removeFromGroupError"),
						msg.getMessage("UpdateRequestsController.partiallyAccepted", accepted),
						null);
			}
		}
	}

	public void decline(String projectPath, Set<UpdateRequestEntry> items) throws ControllerException
	{
		List<String> declined = new ArrayList<>();
		try
		{
			for (UpdateRequestEntry request : items)
			{
				requestMan.decline(new ProjectRequestParam(projectPath, request.id, request.operation, request.type));
				declined.add(request.email.getValue());

			}
		} catch (Exception e)
		{
			log.debug("Can not reject request ", e);
			if (declined.isEmpty())
			{
				throw new ControllerException(
						msg.getMessage("UpdateRequestsController.declineRequestError"),
						msg.getMessage("UpdateRequestsController.notDeclined"), null);
			} else
			{
				throw new ControllerException(
						msg.getMessage("UpdateRequestsController.declineRequestError"),
						msg.getMessage("UpdateRequestsController.partiallyDeclined", declined),
						null);
			}
		}
	}

}
