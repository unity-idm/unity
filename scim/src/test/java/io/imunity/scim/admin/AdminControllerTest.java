/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.admin;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.config.SCIMEndpointConfiguration;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SCIMEndpointPropertiesConfigurationMapper;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class AdminControllerTest
{
	@Mock
	private EndpointManagement endpointManagement;
	@Mock
	private AdminAuthzService adminAuthzService;

	@Test
	public void shouldForwardUpdateEndpointToCoreManager() throws JsonProcessingException, EngineException
	{
		AdminController controller = new AdminController(SCIMEndpointDescription.builder()
				.withMembershipGroups(List.of("/oldMembership")).withEndpointName("scim").build(), endpointManagement,
				adminAuthzService);

		SCIMEndpointConfiguration scimConfig = SCIMEndpointConfiguration.builder().withRestAdminGroup("/")
				.withRootGroup("/").withMembershipGroups(List.of("oldMembership")).withSchemas(Collections.emptyList())
				.build();
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(null, null, null,
				SCIMEndpointPropertiesConfigurationMapper.toProperties(scimConfig), null);
		when(endpointManagement.getEndpoint("scim"))
				.thenReturn(new Endpoint("endpoint", SCIMEndpoint.TYPE.getName(), "/scim", endpointConfiguration, 0));

		controller.updateExposedGroups(
				MembershipGroupsConfiguration.builder().withMembershipGroups(List.of("/membership"))
						.withExcludedMemberhipGroups(List.of("/excluded")).build());

		ArgumentCaptor<EndpointConfiguration> configCaptor = ArgumentCaptor.forClass(EndpointConfiguration.class);
		verify(endpointManagement).updateEndpoint(eq("scim"), configCaptor.capture());

		EndpointConfiguration newConfig = configCaptor.getValue();
		SCIMEndpointConfiguration newScimConfig = SCIMEndpointPropertiesConfigurationMapper
				.fromProperties(newConfig.getConfiguration());
		assertThat(newScimConfig.membershipGroups, is(List.of("/membership")));
		assertThat(newScimConfig.excludedMembershipGroups, is(List.of("/excluded")));
	}

	@Test
	public void shouldSkipUpdateWhenMembershipGroupsNotChanged() throws JsonProcessingException, EngineException
	{
		AdminController controller = new AdminController(
				SCIMEndpointDescription.builder().withMembershipGroups(List.of("/membership"))
						.withExcludedMembershipGroups(List.of("/excluded")).withEndpointName("scim").build(),
				endpointManagement, adminAuthzService);

		SCIMEndpointConfiguration scimConfig = SCIMEndpointConfiguration.builder().withRestAdminGroup("/")
				.withRootGroup("/").withMembershipGroups(List.of("/membership"))
				.withExcludedMembershipGroups(List.of("/excluded")).withSchemas(Collections.emptyList()).build();
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(null, null, null,
				SCIMEndpointPropertiesConfigurationMapper.toProperties(scimConfig), null);
		when(endpointManagement.getEndpoint("scim"))
				.thenReturn(new Endpoint("endpoint", SCIMEndpoint.TYPE.getName(), "/scim", endpointConfiguration, 0));

		controller.updateExposedGroups(
				MembershipGroupsConfiguration.builder().withMembershipGroups(List.of("/membership"))
						.withExcludedMemberhipGroups(List.of("/excluded")).build());

		verify(endpointManagement, times(0)).updateEndpoint(eq("scim"), any());
	}
}
