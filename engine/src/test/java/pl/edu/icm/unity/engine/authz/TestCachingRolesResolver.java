/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.store.api.GroupDAO;

public class TestCachingRolesResolver
{
	private AttributesHelper dbAttributes;
	private GroupDAO groupDAO;
	private Map<String, AuthzRole> rolesMap;
	private AuthzRole r1;

	@Before
	public void init()
	{
		dbAttributes = mock(AttributesHelper.class);
		groupDAO = mock(GroupDAO.class);
		when(groupDAO.exists(anyString())).thenReturn(true);
		rolesMap = new HashMap<>();
		r1 = mock(AuthzRole.class);
		rolesMap.put("role1", r1);
	}
	
	@Test
	public void shouldReturnFromDB() throws EngineException
	{
		CachingRolesResolver resolver = new CachingRolesResolver(rolesMap, dbAttributes, 100000, groupDAO);
		
		AttributeExt role = new AttributeExt(new Attribute(
			RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "string", "/", 
			Lists.newArrayList("role1")), true);
		when(dbAttributes.getAttributeOneGroup(eq(1L), eq("/"), eq(RoleAttributeTypeProvider.AUTHORIZATION_ROLE)))
					.thenReturn(role);
		
		Set<AuthzRole> roles = resolver.establishRoles(1, new Group("/A"));
		
		assertThat(roles.size(), is(1));
		assertThat(roles, hasItem(r1));
	}

	@Test
	public void shouldReturnFromCache() throws EngineException
	{
		CachingRolesResolver resolver = new CachingRolesResolver(rolesMap, dbAttributes, 100000, groupDAO);
		
		AttributeExt role = new AttributeExt(new Attribute(
				RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "string", "/", 
				Lists.newArrayList("role1")), true);
		when(dbAttributes.getAttributeOneGroup(
				eq(1L), eq("/"), eq(RoleAttributeTypeProvider.AUTHORIZATION_ROLE))).thenReturn(role);
		
		resolver.establishRoles(1, new Group("/A"));
		Set<AuthzRole> roles = resolver.establishRoles(1, new Group("/A"));
		
		assertThat(roles.size(), is(1));
		assertThat(roles, hasItem(r1));
		verify(dbAttributes).getAttributeOneGroup(eq(1L), eq("/"), eq(RoleAttributeTypeProvider.AUTHORIZATION_ROLE));
		verify(dbAttributes).getAttributeOneGroup(eq(1L), eq("/A"), eq(RoleAttributeTypeProvider.AUTHORIZATION_ROLE));
	}

	
	@Test
	public void shouldExpireCache() throws Exception
	{
		CachingRolesResolver resolver = new CachingRolesResolver(rolesMap, dbAttributes, 1, groupDAO);

		AttributeExt role = new AttributeExt(new Attribute(
				RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "string", "/", 
				Lists.newArrayList("role1")), true);
		when(dbAttributes.getAttributeOneGroup(
				eq(1L), eq("/"), eq(RoleAttributeTypeProvider.AUTHORIZATION_ROLE))).thenReturn(role);

		resolver.establishRoles(1, new Group("/A"));
		Thread.sleep(10);
		Set<AuthzRole> roles = resolver.establishRoles(1, new Group("/A"));

		assertThat(roles.size(), is(1));
		assertThat(roles, hasItem(r1));
		verify(dbAttributes, times(2)).getAttributeOneGroup(eq(1L), eq("/"), eq(RoleAttributeTypeProvider.AUTHORIZATION_ROLE));
		verify(dbAttributes, times(2)).getAttributeOneGroup(eq(1L), eq("/A"), eq(RoleAttributeTypeProvider.AUTHORIZATION_ROLE));
	}

}
