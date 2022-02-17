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

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
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
				"/scim", List.of("/scim/Members1", "/scim/Members2"), Collections.emptyList());
		userAuthzService = new UserAuthzService(authzMan, configuration);
	}

	@Test
	public void shouldBlockAccessToUserWhenCallerHasNoReadCapabilityOnAttrRootGroup() throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(false), eq("/scim"));
		Throwable error = Assertions.catchThrowable(() -> userAuthzService.checkReadUser(1));
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldBlockSeflAccessToUserWhenCallerHasNoReadCapabilityOnAttrRootGroup() throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 1, null, null, null, null));
		InvocationContext.setCurrent(context);
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(true), eq("/scim"));
		Throwable error = Assertions.catchThrowable(() -> userAuthzService.checkReadUser(1));
		Assertions.assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shoulAcceptAccessToUserWhenCallerHasReadCapabilityOnAttrRootGroup() throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);
		try
		{
			userAuthzService.checkReadUser(1);
		} catch (Exception e)
		{
			fail();
		}

	}

	@Test
	public void shoulAcceptSelfAccessToUserWhenCallerHasReadCapabilityOnAttrRootGroup() throws AuthorizationException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 1, null, null, null, null));
		InvocationContext.setCurrent(context);
		try
		{
			userAuthzService.checkReadUser(1);
		} catch (Exception e)
		{
			fail();
		}

	}
}
