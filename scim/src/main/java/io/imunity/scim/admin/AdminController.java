/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.admin;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.scim.admin.AdminAuthzService.SCIMAdminAuthzServiceFactory;
import io.imunity.scim.config.SCIMEndpointConfiguration;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SCIMEndpointPropertiesConfigurationMapper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

class AdminController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, AdminController.class);

	private final SCIMEndpointDescription configuration;
	private final EndpointManagement endpointManagement;
	private final AdminAuthzService adminAuthzService;

	AdminController(SCIMEndpointDescription configuration, EndpointManagement endpointManagement,
			AdminAuthzService adminAuthzService)
	{
		this.configuration = configuration;
		this.endpointManagement = endpointManagement;
		this.adminAuthzService = adminAuthzService;
	}

	public MembershipGroupsConfiguration getExposedGroups() throws EngineException
	{
		adminAuthzService.authorizeReadOrUpdateOfExposedGroups();
		Endpoint endpoint = endpointManagement.getEndpoint(configuration.endpointName);
		EndpointConfiguration endpointConfiguration = endpoint.getConfiguration();
		SCIMEndpointConfiguration currentScimEndpointConfig = SCIMEndpointPropertiesConfigurationMapper
				.fromProperties(endpointConfiguration.getConfiguration());

		return MembershipGroupsConfiguration.builder()
				.withExcludedMemberhipGroups(currentScimEndpointConfig.excludedMembershipGroups)
				.withMembershipGroups(currentScimEndpointConfig.membershipGroups).build();
	}

	void updateExposedGroups(MembershipGroupsConfiguration config) throws JsonProcessingException, EngineException
	{
		adminAuthzService.authorizeReadOrUpdateOfExposedGroups();

		Endpoint endpoint = endpointManagement.getEndpoint(configuration.endpointName);

		EndpointConfiguration endpointConfiguration = endpoint.getConfiguration();

		SCIMEndpointConfiguration currentScimEndpointConfig = SCIMEndpointPropertiesConfigurationMapper
				.fromProperties(endpointConfiguration.getConfiguration());

		SCIMEndpointConfiguration updatedScimEndpointConfig = SCIMEndpointConfiguration
				.builder(currentScimEndpointConfig).withMembershipGroups(config.membershipGroups)
				.withExcludedMembershipGroups(config.excludedMemberhipGroups).build();
		if (currentScimEndpointConfig.equals(updatedScimEndpointConfig))
		{
			log.debug(
					"Skipping update membership groups configuration for SCIM endpoint {} , the groups remains unchanged",
					configuration.endpointName);
			return;
		}

		EndpointConfiguration updatedEndpointConfiguration = new EndpointConfiguration(
				endpointConfiguration.getDisplayedName(), endpointConfiguration.getDescription(),
				endpointConfiguration.getAuthenticationOptions(),
				SCIMEndpointPropertiesConfigurationMapper.toProperties(updatedScimEndpointConfig),
				endpointConfiguration.getRealm(), endpointConfiguration.getTag());

		log.debug("Update SCIM endpoint {}, set membership groups={}, excludedMembershipGroups={}",
				configuration.endpointName, updatedScimEndpointConfig.membershipGroups,
				updatedScimEndpointConfig.excludedMembershipGroups);
		endpointManagement.updateEndpoint(configuration.endpointName, updatedEndpointConfiguration);

	}

	@Component
	static class AdminControllerFactory
	{
		private final EndpointManagement endpointManagement;
		private final SCIMAdminAuthzServiceFactory adminAuthzServiceFactory;

		@Autowired
		AdminControllerFactory(@Qualifier("insecure") EndpointManagement endpointManagement,
				SCIMAdminAuthzServiceFactory adminAuthzServiceFactory)
		{
			this.endpointManagement = endpointManagement;
			this.adminAuthzServiceFactory = adminAuthzServiceFactory;
		}

		AdminController getService(SCIMEndpointDescription configuration)
		{
			return new AdminController(configuration, endpointManagement,
					adminAuthzServiceFactory.getService(configuration));
		}
	}
}
