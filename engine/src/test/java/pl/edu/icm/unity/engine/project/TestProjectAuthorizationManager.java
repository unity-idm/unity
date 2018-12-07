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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class TestProjectAuthorizationManager
{
	@Mock
	GroupDAO mockGroupDao;

	@Mock
	AttributeDAO mockAttrDao;

	@Test
	public void shouldThrowAuthzExceptionWhenDelegationIsNotEnabled()
	{
		assertAuthzException(checkAuthz(false, GroupAuthorizationRole.manager));

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

		Group group = new Group("/project");
		group.setDelegationConfiguration(new GroupDelegationConfiguration(groupWithEnabledDelegation));

		when(mockGroupDao.get(anyString())).thenReturn(group);
		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project")))
				.thenReturn(Arrays.asList(new StoredAttribute(new AttributeExt(
						new Attribute(null, null, null, Arrays.asList(userRole.toString())),
						false), 1L)));

		return catchThrowable(() -> mockAuthz.checkManagerAuthorization("/project"));

	}

	private void assertAuthzException(Throwable exception)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(AuthorizationException.class);
	}

}
