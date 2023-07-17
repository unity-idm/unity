/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.scim.SCIMSystemScopeProvider;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;

@ExtendWith(MockitoExtension.class)
public class GroupAuthzServiceTest
{
	@Mock
	private AuthorizationManagement authzMan;

	@Mock
	private EntityManagement entityManagement;

	private GroupAuthzService groupAuthzService;

	@BeforeEach
	public void init()
	{
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/scim/Members1", "/scim/Members2"))
				.build();
		groupAuthzService = new GroupAuthzService(authzMan, entityManagement, configuration);
	}

	@Test
	public void shouldBlockReadGroupsWhenUserHasNoReadCapabilityOnAttrRootGroup() throws AuthorizationException
	{
		setupInvocationContext(InvocationMaterial.DIRECT);
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(false), eq("/scim"));
		Throwable error = Assertions.catchThrowable(() -> groupAuthzService.checkReadGroups());
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldBlockReadGroupsWhenOAuthInvocationMaterialAndNoReadMemberScope() throws AuthorizationException
	{
		setupInvocationContext(InvocationMaterial.OAUTH_DELEGATION);
		Throwable error = Assertions.catchThrowable(() -> groupAuthzService.checkReadGroups());
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldAcceptReadGroupsWhenOAuthInvocationMaterialAndReadMemberScope() throws AuthorizationException
	{
		setupInvocationContext(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext context = InvocationContext.getCurrent();
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_SELF_GROUP_SCOPE));
		try
		{
			groupAuthzService.checkReadGroups();
		} catch (Exception e)
		{
			fail();
		}
	}

	@Test
	public void shouldNotFilterGroupWhenDirectInvocationMaterial() throws EngineException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);
		Predicate<String> filter = groupAuthzService.getFilter();
		assertThat(filter.test("/scim/1")).isTrue();
	}

	@Test
	public void shouldFilterOnlyUserGroupWhenOAuthInvocationMaterial() throws EngineException
	{
		setupInvocationContext(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext context = InvocationContext.getCurrent();
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_SELF_GROUP_SCOPE));
		when(entityManagement.getGroups(new EntityParam(1L)))
				.thenReturn(Map.of("/userGroup", new GroupMembership("/userGroup", 1, new Date())));
		Predicate<String> filter = groupAuthzService.getFilter();

		assertThat(filter.test("/userGroup")).isTrue();
		assertThat(filter.test("/notUserGroup")).isFalse();

	}

	@Test
	public void shouldAcceptReadGroupsWhenUserHasReadCapabilityOnAttrRootGroup() throws AuthorizationException
	{
		try
		{
			groupAuthzService.checkReadGroups();
		} catch (Exception e)
		{
			fail();
		}
	}

	private void setupInvocationContext(InvocationMaterial invocationMaterial)
	{
		InvocationContext invContext = new InvocationContext(null, null, null);
		invContext.setInvocationMaterial(invocationMaterial);
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		InvocationContext.setCurrent(invContext);
	}
}
