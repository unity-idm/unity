/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;

@RunWith(MockitoJUnitRunner.class)
public class GroupAuthzServiceTest
{
	@Mock
	private AuthorizationManagement authzMan;

	private GroupAuthzService groupAuthzService;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https//localhost:2443/scim"),
				"/scim", List.of("/scim/Members1", "/scim/Members2"));
		groupAuthzService = new GroupAuthzService(authzMan, configuration);
	}

	@Test
	public void shoulBlockReadGroups() throws AuthorizationException
	{
		doThrow(AuthorizationException.class).when(authzMan).checkReadCapability(eq(false), eq("/scim"));
		catchException(groupAuthzService).checkReadGroups();
		assertThat(caughtException(), isA(AuthorizationException.class));
	}

	@Test
	public void shoulAcceptReadGroups() throws AuthorizationException
	{
		try
		{
			groupAuthzService.checkReadGroups();
		} catch (Exception e)
		{
			fail();
		}

	}

}
