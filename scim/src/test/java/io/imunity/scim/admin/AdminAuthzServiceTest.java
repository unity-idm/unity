/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.admin;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;

@RunWith(MockitoJUnitRunner.class)
public class AdminAuthzServiceTest
{
	@Mock
	private EntityManagement entityManagement;

	@Test
	public void shouldAcceptMembershipGroupChangingWhenUserIsInAdminGroup() throws EngineException
	{
		setupInvocationContext(InvocationMaterial.DIRECT);
		AdminAuthzService adminAuthzService = new AdminAuthzService(
				SCIMEndpointDescription.builder().withRestAdminGroup("/admin").build(), entityManagement);
		when(entityManagement.getGroups(new EntityParam(1L)))
				.thenReturn(Map.of("/admin", new GroupMembership("/admin", 1, new Date())));
		try
		{
			adminAuthzService.authorizeReadOrUpdateOfExposedGroups();
		} catch (Exception e)
		{
			fail();
		}
	}

	@Test
	public void shouldNotAcceptMembershipGroupChangingWhenUserIsNotInAdminGroup() throws EngineException
	{
		setupInvocationContext(InvocationMaterial.DIRECT);
		AdminAuthzService adminAuthzService = new AdminAuthzService(
				SCIMEndpointDescription.builder().withRestAdminGroup("/admin").build(), entityManagement);
		when(entityManagement.getGroups(new EntityParam(1L)))
				.thenReturn(Map.of("/", new GroupMembership("/", 1, new Date())));
		Throwable error = Assertions.catchThrowable(() -> adminAuthzService.authorizeReadOrUpdateOfExposedGroups());
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldNotAcceptMembershipGroupChangingWhenNotDirectInvocationMaterial() throws EngineException
	{
		setupInvocationContext(InvocationMaterial.OAUTH_DELEGATION);
		AdminAuthzService adminAuthzService = new AdminAuthzService(
				SCIMEndpointDescription.builder().withRestAdminGroup("/admin").build(), entityManagement);
		Throwable error = Assertions.catchThrowable(() -> adminAuthzService.authorizeReadOrUpdateOfExposedGroups());
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}
	
	@Test
	public void shouldThrowAuthorizationExceptionWhenRestAdminGroupIsNotSet() throws EngineException
	{
		AdminAuthzService adminAuthzService = new AdminAuthzService(
				SCIMEndpointDescription.builder().build(), entityManagement);
		Throwable error = Assertions.catchThrowable(() -> adminAuthzService.authorizeReadOrUpdateOfExposedGroups());
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	private void setupInvocationContext(InvocationMaterial invocationMaterial)
	{
		InvocationContext invContext = new InvocationContext(null, null, null);
		invContext.setInvocationMaterial(invocationMaterial);
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		InvocationContext.setCurrent(invContext);
	}
}
