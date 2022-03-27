/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.SCIMSystemScopeProvider;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.exceptions.AuthorizationException;

@RunWith(MockitoJUnitRunner.class)
public class UserAuthzServiceTest
{
	@Mock
	private AuthorizationManagement authzMan;

	private UserAuthzService userAuthzService;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https//localhost:2443/scim"),
				"/scim", List.of("/scim/Members1", "/scim/Members2"), Collections.emptyList(), Collections.emptyList());
		userAuthzService = new UserAuthzService(authzMan, configuration);
	}

	@Test
	public void shouldBlockAccessToUserWhenDirectInvocationMaterialAndCallerHasNoReadCapabilityOnAttrRootGroup()
			throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2l, null, null, null, null));
		InvocationContext.setCurrent(context);
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(false), eq("/scim"));
		Throwable error = Assertions.catchThrowable(() -> userAuthzService.checkReadUser(1l, Collections.emptySet()));
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
			userAuthzService.checkReadUser(1l, Set.of("/scim"));
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
		Throwable error = Assertions.catchThrowable(() -> userAuthzService.checkReadUser(1l, Collections.emptySet()));
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
			userAuthzService.checkReadUser(1l, Collections.emptySet());
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
			userAuthzService.checkReadUser(1l, Collections.emptySet());
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
		Throwable error = Assertions.catchThrowable(() -> userAuthzService.checkReadUser(1l, Collections.emptySet()));
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
		Throwable error = Assertions.catchThrowable(() -> userAuthzService.checkReadUser(2l, Collections.emptySet()));
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
			userAuthzService.checkReadUser(1l, Set.of("/scim"));
		} catch (Exception e)
		{
			fail();
		}
	}
}
