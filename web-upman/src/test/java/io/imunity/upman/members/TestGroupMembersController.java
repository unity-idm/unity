/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;

import io.imunity.upman.utils.DelegatedGroupsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TestGroupMembersController
{
	@Mock
	private UnityMessageSource mockMsg;

	@Mock
	private DelegatedGroupManagement mockDelGroupMan;

	@Mock
	private DelegatedGroupsHelper mockDelGroupHelper;

	@Mock
	private AttributeHandlerRegistry mockAttrHandlerRegistry;

	private GroupMembersController controller;

	@Before
	public void initController()
	{
		controller = new GroupMembersController(mockMsg, mockAttrHandlerRegistry, mockDelGroupMan,
				mockDelGroupHelper);
	}

	@Test
	public void shouldForwardGerMembersToCoreManager() throws ControllerException, EngineException
	{
		controller.getGroupMembers("/project", "/project/group");
		verify(mockDelGroupMan).getDelegatedGroupMemebers(eq("/project"), eq("/project/group"));

	}

	@Test
	public void shouldForwardAddMemberToCoreManager() throws ControllerException, EngineException
	{
		controller.addToGroup("/project", "/project/group", Sets.newHashSet(getMember()));
		verify(mockDelGroupMan).addMemberToGroup(eq("/project"), eq("/project/group"), eq(1L));
	}

	@Test
	public void shouldForwardRemoveMemberToCoreManager() throws ControllerException, EngineException
	{
		controller.removeFromGroup("/project", "/project/group", Sets.newHashSet(getMember()));
		verify(mockDelGroupMan).removeMemberFromGroup(eq("/project"), eq("/project/group"), eq(1L));

	}

	@Test
	public void shouldForwardSetManagerPriviligesToCoreManager() throws ControllerException, EngineException
	{
		controller.addManagerPrivileges("/project", Sets.newHashSet(getMember()));
		verify(mockDelGroupMan).setGroupAuthorizationRole(eq("/project"), eq(1L),
				eq(GroupAuthorizationRole.manager));

	}

	@Test
	public void shouldForwardRevokeManagerPriviligesToCoreManager() throws ControllerException, EngineException
	{
		controller.revokeManagerPrivileges("/project", Sets.newHashSet(getMember()));
		verify(mockDelGroupMan).setGroupAuthorizationRole(eq("/project"), eq(1L),
				eq(GroupAuthorizationRole.regular));

	}

	@Test
	public void shouldForwardGetAdditinalAttributesToCoreManager() throws EngineException, ControllerException
	{

		DelegatedGroup delGroup = new DelegatedGroup("/project", new GroupDelegationConfiguration(true, null,
				null, null, null, Arrays.asList("extraAttr")), true, "name");

		DelegatedGroupContents con = new DelegatedGroupContents(delGroup, Optional.empty());

		when(mockDelGroupMan.getContents(eq("/project"), eq("/project"))).thenReturn(con);
		when(mockDelGroupMan.getAttributeDisplayedName(eq("/project"), eq("extraAttr"))).thenReturn("extra");

		Map<String, String> additionalAttributeNamesForProject = controller
				.getAdditionalAttributeNamesForProject("/project");
		assertThat(additionalAttributeNamesForProject.isEmpty(), is(false));
		assertThat(additionalAttributeNamesForProject.get("extraAttr"), is("extra"));

	}

	@Test
	public void shouldForwardGetProjectGroupsToHelper() throws ControllerException, EngineException
	{
		controller.getProjectGroups("/project");
		verify(mockDelGroupHelper).getProjectGroups(eq("/project"));
	}

	private GroupMemberEntry getMember()
	{
		return new GroupMemberEntry(new DelegatedGroupMember(1L, null, null, null, null, null, Optional.empty()), null);
	}

}
