/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.home;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.ProjectManagementConstants;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class simplifies getting project management endpoint address based on home ui config
 * @author P.Piernik
 *
 */
@Component
public class ProjectManagementHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ProjectManagementHelper.class);
	
	private final DelegatedGroupManagement delGroupMan;
	private EndpointManagement endpointMan;
	private AdvertisedAddressProvider advertisedAddrProvider;
	
	@Autowired
	public ProjectManagementHelper(DelegatedGroupManagement delGroupMan,
			@Qualifier("insecure") EndpointManagement endpointMan,
			AdvertisedAddressProvider advertisedAddrProvider)
	{
		this.delGroupMan = delGroupMan;
		this.endpointMan = endpointMan;
		this.advertisedAddrProvider = advertisedAddrProvider;
	}
	
	public Optional<String> getProjectManLinkIfAvailable(HomeEndpointProperties config)
	{

		if (!config.isProjectManLinkIsEnabled())
			return Optional.empty();

		Set<ResolvedEndpoint> allEndpoints = getAllProjectManEndpoints();
		if (allEndpoints.isEmpty())
		{
			log.debug("Project mamangement link is enabled, but project management endpoins are not available");
			return Optional.empty();

		}

		if (!checkIfUserHasProjects())
		{
			log.debug("Project mamangement link is enabled, but user is not a manager of any group");
			return Optional.empty();
		}

		String projectManEndpointName = config.getProjectManEndpoint();

		if (projectManEndpointName == null)
		{
			log.debug("Project mamangement link is enabled, using first available project management endpoint");
			return Optional.ofNullable(getLinkToProjectManagementEndpoint(allEndpoints.iterator().next()));
		} else
		{

			ResolvedEndpoint endpoint = allEndpoints.stream()
					.filter(e -> e.getName().equals(config.getProjectManEndpoint())).findAny()
					.orElse(null);

			if (endpoint == null)
			{
				log.warn("Project mamangement link is enabled, but endpoint with name "
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
		return advertisedAddrProvider.get() + endpoint.getEndpoint().getContextAddress();
	}

	public Set<ResolvedEndpoint> getAllProjectManEndpoints()
	{
		try
		{
			return endpointMan.getDeployedEndpoints().stream().filter(
					e -> e.getType().getName().equals(ProjectManagementConstants.ENDPOINT_NAME))
					.collect(Collectors.toSet());
		} catch (EngineException e)
		{
			log.error("Can not get project management endpoints", e);
			return Collections.emptySet();
		}

	}
}
