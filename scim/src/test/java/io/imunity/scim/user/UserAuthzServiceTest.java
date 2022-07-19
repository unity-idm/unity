/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.SCIMSystemScopeProvider;
import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.AuthorizationException;

@RunWith(MockitoJUnitRunner.class)
public class UserAuthzServiceTest
{
	@Mock
	private AuthorizationManagement authzMan;

	@Test
	public void shouldBlockAccessToUserWhenDirectInvocationMaterialAndCallerHasNoReadCapabilityOnAttrRootGroup()
			throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2l, null, null, null, null));
		InvocationContext.setCurrent(context);
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(false), eq("/scim"));
		Throwable error = Assertions.catchThrowable(() -> getAuthzService().checkReadUser(1l, Collections.emptySet()));
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldAcceptAccessToSelfWhenDirectInvocationMaterialAndCallerHasNoReadCapabilityOnAttrRootGroup()
			throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 1l, null, null, null, null));
		InvocationContext.setCurrent(context);
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(true), eq("/scim"));
		try
		{
			getAuthzService().checkReadUser(1l, Set.of("/scim"));
		} catch (Exception e)
		{
			fail();
		}
	}

	@Test
	public void shouldBlockSeflAccessToUserWhenDirectInvocationMaterialAndCallerIsNotInAttrRootGroup()
			throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 1, null, null, null, null));
		InvocationContext.setCurrent(context);
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(true), eq("/scim"));
		Throwable error = Assertions.catchThrowable(() -> getAuthzService().checkReadUser(1l, Collections.emptySet()));
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldAcceptAccessToUserWhenDirectInvocationMaterialAndCallerHasReadCapabilityOnAttrRootGroup()
			throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);
		try
		{
			getAuthzService().checkReadUser(1l, Collections.emptySet());
		} catch (Exception e)
		{
			fail();
		}

	}

	@Test
	public void shouldAcceptSelfAccessToUserWhenDirectInvocationMaterialAndCallerHasReadCapabilityOnAttrRootGroup()
			throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 1, null, null, null, null));
		InvocationContext.setCurrent(context);
		try
		{
			getAuthzService().checkReadUser(1l, Collections.emptySet());
		} catch (Exception e)
		{
			fail();
		}
	}

	@Test
	public void shouldBlockAccessToUserWhenOAuthInvocationMaterialAndEmptyScopes() throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.setCurrent(context);
		Throwable error = Assertions.catchThrowable(() -> getAuthzService().checkReadUser(1l, Collections.emptySet()));
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldBlockSelfAccessWhenOAuthInvocationMaterialAndOtherUser() throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 1l, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_PROFILE_SCOPE));
		InvocationContext.setCurrent(context);
		Throwable error = Assertions.catchThrowable(() -> getAuthzService().checkReadUser(2l, Collections.emptySet()));
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldAcceptSelfAccessToUserWhenOAuthInvocationMaterialAndScimScope() throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 1, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_PROFILE_SCOPE));
		InvocationContext.setCurrent(context);

		try
		{
			getAuthzService().checkReadUser(1l, Set.of("/scim"));
		} catch (Exception e)
		{
			fail();
		}
	}
	
	@Test
	public void shouldNotFilterWhenDirectInvocationMaterial()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"), Collections.emptyList());

		UserAuthzService attributeFilterService = new UserAuthzService(authzMan, configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("allow").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(true));
	}

	@Test
	public void shouldAllowOnlyGroupMembershipAttrsWhenOAuthInvocationMaterialAndReadMembershipScope()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_MEMBERSHIPS_SCOPE));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"), Collections.emptyList());

		UserAuthzService attributeFilterService = new UserAuthzService(authzMan, configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("deny").build()).build()),
				is(false));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr2").build()).build()),
				is(true));
	}

	@Test
	public void shouldAllowOnlyNotGroupMembershipAttrsWhenOAuthInvocationMaterialAndReadProfileScope()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_PROFILE_SCOPE));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"), Collections.emptyList());

		UserAuthzService attributeFilterService = new UserAuthzService(authzMan, configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("allow").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(false));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr2").build()).build()),
				is(false));
	}

	@Test
	public void shouldNotFilterWhenOAuthInvocationMaterialAndAllScimScopes()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_PROFILE_SCOPE, SCIMSystemScopeProvider.READ_MEMBERSHIPS_SCOPE));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"), Collections.emptyList());

		UserAuthzService attributeFilterService = new UserAuthzService(authzMan, configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("allow").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr2").build()).build()),
				is(true));
	}
	
	private UserAuthzService getAuthzService()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https//localhost:2443/scim"),
				"/scim", List.of("/scim/Members1", "/scim/Members2"), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		return new UserAuthzService(authzMan, configuration);
	}
}
