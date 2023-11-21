/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(MockitoExtension.class)
public class TestProjectAuthorizationManager
{
	@Mock
	GroupDAO mockGroupDao;

	@Mock
	AttributeDAO mockAttrDao;

	@Test
	public void shouldThrowAuthzExceptionWhenDelegationIsNotEnabled()
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);
		addGroup("/project", false);
		assertAuthzException(catchThrowable(() -> mockAuthz.assertManagerAuthorization("/project")));
	}

	@Test
	public void shouldThrowAuthzExceptionWhenRegularUser()
	{
		assertAuthzException(checkAuthz(true, GroupAuthorizationRole.regular));

	}

	@Test
	public void shouldAcceptAuthzWhenManagerInEnabledGroupCall() throws AuthorizationException
	{
		Throwable ex = checkAuthz(true, GroupAuthorizationRole.manager);
		Assertions.assertThat(ex).isNull();

	}

	@Test
	public void shouldAcceptAuthzWhenProjectsAdminSetManagerInSubgroup() throws AuthorizationException
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", true);
		addGroup("/project/sub", true);

		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project")))
				.thenReturn(Arrays.asList(new StoredAttribute(new AttributeExt(
						new Attribute(null, null, null, Arrays.asList(
								GroupAuthorizationRole.projectsAdmin.toString())),
						false), 1L)));
		Throwable ex = catchThrowable(
				() -> mockAuthz.assertRoleManagerAuthorization("/project", "/project/sub", GroupAuthorizationRole.manager));
		Assertions.assertThat(ex).isNull();
	}
	
	@Test
	public void shouldAcceptAuthzWhenManagerSetManagerInDirectSubgroup() throws AuthorizationException
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", true);
		addGroup("/project/sub", true);

		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project")))
				.thenReturn(Arrays.asList(new StoredAttribute(new AttributeExt(
						new Attribute(null, null, null, Arrays.asList(
								GroupAuthorizationRole.manager.toString())),
						false), 1L)));
		Throwable ex = catchThrowable(
				() -> mockAuthz.assertRoleManagerAuthorization("/project", "/project/sub", GroupAuthorizationRole.manager));
		assertAuthzException(ex);
	}
	
	@Test
	public void shouldThrowAuthzExceptionWhenManagerSetsProjectsAdminInSubgroup() throws AuthorizationException
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", true);
		addGroup("/project/sub", true);

		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project")))
				.thenReturn(Arrays.asList(new StoredAttribute(new AttributeExt(
						new Attribute(null, null, null, Arrays.asList(
								GroupAuthorizationRole.manager.toString())),
						false), 1L)));
		Throwable ex = catchThrowable(
				() -> mockAuthz.assertRoleManagerAuthorization("/project", "/project/sub", GroupAuthorizationRole.projectsAdmin));
		assertAuthzException(ex);
	}
	
	@Test
	public void shouldThrowAuthzExceptionWhenManagerSetManagerInFutherSubgroup() throws AuthorizationException
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", true);
		addGroup("/project/sub/sub2", true);

		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project"))).thenReturn(Arrays
				.asList(new StoredAttribute(new AttributeExt(new Attribute(null, null, null, Arrays
						.asList(GroupAuthorizationRole.manager.toString())),
						false), 1L)));
		Throwable ex = catchThrowable(
				() -> mockAuthz.assertRoleManagerAuthorization("/project", "/project/sub/sub2", GroupAuthorizationRole.manager));
		assertAuthzException(ex);
	}
	
	@Test
	public void shouldThrowAuthzExceptionWhenRegularSetManagerInSubgroup() throws AuthorizationException
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", true);
		addGroup("/project/sub", true);

		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project")))
				.thenReturn(Arrays.asList(new StoredAttribute(new AttributeExt(
						new Attribute(null, null, null, Arrays.asList(
								GroupAuthorizationRole.regular.toString())),
						false), 1L)));
		Throwable ex = catchThrowable(
				() -> mockAuthz.assertRoleManagerAuthorization("/project", "/project/sub", GroupAuthorizationRole.manager));
		assertAuthzException(ex);
	}
	
	@Test
	public void shouldBlockCreationWhenDisabledSubprojectInConfig() throws AuthorizationException
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", true);
		
		Throwable ex = catchThrowable(
				() -> mockAuthz.assertProjectsAdminAuthorization("/project", "/project/sub"));
		assertAuthzException(ex);
	}
	
	@Test
	public void shouldBlockCreationWhenNotProjectsAdmin() throws AuthorizationException
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", true, true);

		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project")))
				.thenReturn(Arrays.asList(new StoredAttribute(new AttributeExt(
						new Attribute(null, null, null, Arrays.asList(
								GroupAuthorizationRole.manager.toString())),
						false), 1L)));
		Throwable ex = catchThrowable(
				() -> mockAuthz.assertProjectsAdminAuthorization("/project", "/project/sub"));
		assertAuthzException(ex);
	}

	private void assertAuthzException(Throwable exception)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(AuthorizationException.class);
	}

	private void addGroup(String path, boolean groupWithEnabledDelegation)
	{
		addGroup(path, groupWithEnabledDelegation, false);
	}
	
	
	private void addGroup(String path, boolean groupWithEnabledDelegation, boolean enableSubproject)
	{
		Group group = new Group(path);
		group.setDelegationConfiguration(new GroupDelegationConfiguration(groupWithEnabledDelegation, 
				enableSubproject, null, null, null, null, List.of(), List.of()));
		when(mockGroupDao.get(eq(path))).thenReturn(group);
	}

	private void setupInvocationContext()
	{
		InvocationContext invContext = new InvocationContext(null, null, null);
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		InvocationContext.setCurrent(invContext);
	}

	private Throwable checkAuthz(boolean groupWithEnabledDelegation, GroupAuthorizationRole userRole)
	{
		setupInvocationContext();
		ProjectAuthorizationManager mockAuthz = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);

		addGroup("/project", groupWithEnabledDelegation);

		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project")))
				.thenReturn(Arrays.asList(new StoredAttribute(new AttributeExt(
						new Attribute(null, null, null, Arrays.asList(userRole.toString())),
						false), 1L)));

		return catchThrowable(() -> mockAuthz.assertManagerAuthorization("/project"));

	}
}
