/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webadmin.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.ProjectManagementConstants;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * Utility class simplifies getting project management endpoint address based on home ui config
 * @author P.Piernik
 *
 */
@Component
public class ProjectManagementHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ProjectManagementHelper.class);
	
	private DelegatedGroupManagement delGroupMan;
	private EndpointManagement endpointMan;
	private NetworkServer networkServer;
	
	@Autowired
	public ProjectManagementHelper(DelegatedGroupManagement delGroupMan, @Qualifier("insecure") EndpointManagement endpointMan,
			NetworkServer networkServer)
	{
		this.delGroupMan = delGroupMan;
		this.endpointMan = endpointMan;
		this.networkServer = networkServer;
	}
	
	public Optional<String> getProjectManLinkIfAvailable(HomeEndpointProperties config)
	{

		if (!config.isProjectManLinkIsEnabled())
			return Optional.empty();

		Set<ResolvedEndpoint> allEndpoints = getAllProjectManEndpoints();
		if (allEndpoints.isEmpty())
		{
			log.debug("Project mamangement link is enable, but project management endpoins are not available");
			return Optional.empty();

		}

		if (!checkIfUserHasProjects())
		{
			log.debug("Project mamangement link is enable, but user is not a manager of any group");
			return Optional.empty();
		}

		String projectManEndpointName = config.getProjectManEndpoint();

		if (projectManEndpointName == null)
		{
			log.debug("Project mamangement link is enable, using first available project management endpoint");
			return Optional.ofNullable(getLinkToProjectManagementEndpoint(allEndpoints.iterator().next()));
		} else
		{

			ResolvedEndpoint endpoint = allEndpoints.stream()
					.filter(e -> e.getName().equals(config.getProjectManEndpoint())).findAny()
					.orElse(null);

			if (endpoint == null)
			{
				log.debug("Project mamangement link is enable, but endpoint with name "
						+ projectManEndpointName + " is not available");
				return Optional.empty();
			}

			return Optional.ofNullable(getLinkToProjectManagementEndpoint(endpoint));
		}
	}

	private boolean checkIfUserHasProjects()

	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		if (entity == null)
			return false;
		List<DelegatedGroup> projectsForEntity;
		try
		{
			projectsForEntity = delGroupMan.getProjectsForEntity(entity.getEntityId());
		} catch (EngineException e)
		{
			log.error("Can not get projects for entity " + entity, e);
			return false;
		}

		return !projectsForEntity.isEmpty();
	}

	private String getLinkToProjectManagementEndpoint(ResolvedEndpoint endpoint)
	{
		if (endpoint == null)
			return null;
		return networkServer.getAdvertisedAddress() + endpoint.getEndpoint().getContextAddress();
	}

	private Set<ResolvedEndpoint> getAllProjectManEndpoints()
	{
		try
		{
			return endpointMan.getEndpoints().stream().filter(
					e -> e.getType().getName().equals(ProjectManagementConstants.ENDPOINT_NAME))
					.collect(Collectors.toSet());
		} catch (EngineException e)
		{
			log.error("Can not get project management endpoints", e);
			return Collections.emptySet();
		}

	}
}
