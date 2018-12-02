/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.IllegalGroupNameException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.RemovalOfProjectGroupException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;

@RunWith(MockitoJUnitRunner.class)
public class TestDelegatedGroupManagement
{

	@Mock
	ProjectAuthorizationManager mockAuthz;

	@Mock
	GroupsManagement mockGroupMan;

	@Mock
	BulkGroupQueryService mockBulkQueryService;

	@Mock
	UnityMessageSource mockMsg;

	@Test
	public void shouldThrowIllegalGroupName() throws EngineException
	{

		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);
		GroupContents con = new GroupContents();
		con.setGroup(new Group("/project1"));
		con.setSubGroups(Lists.emptyList());
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);

		Throwable exception = catchThrowable(
				() -> dGroupMan.addGroup("/project1", "project1/subgroup", new I18nString(), false));

		assertException(exception, IllegalGroupNameException.class);
	}

	@Test
	public void shouldAddGroup() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);
		GroupContents con = new GroupContents();
		con.setGroup(new Group("/project1"));
		con.setSubGroups(Lists.emptyList());
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);

		I18nString groupName = new I18nString("GroupName");
		dGroupMan.addGroup("/project1", "project1/subgroup", groupName, false);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).addGroup(argument.capture());

		assertThat(argument.getValue().getDisplayedName(), is(groupName));

	}

	@Test
	public void shouldThrowRemovalOfProjectGroupException() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		Throwable exception = catchThrowable(() -> dGroupMan.removeGroup("/project1", "/project1"));

		assertException(exception, RemovalOfProjectGroupException.class);
	}

	@Test
	public void shouldRemoveGroup() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		dGroupMan.removeGroup("/project1", "/project1/group1");

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockGroupMan).removeGroup(argument.capture(), eq(true));

		assertThat(argument.getValue(), is("/project1/group1"));
	}

	@Test
	public void shouldReturnGroupAndSubgroups() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, null,
				mockBulkQueryService, null, null, null, null, null, mockAuthz);

		when(mockBulkQueryService.getBulkStructuralData(anyString())).thenReturn(null);

		Map<String, GroupContents> ret = new HashMap<>();

		GroupContents p = new GroupContents();
		p.setGroup(new Group("/project"));
		p.setSubGroups(Lists.list("/project/subgroup"));
		ret.put("/project", p);

		GroupContents g = new GroupContents();
		g.setGroup(new Group("/project/subgroup"));
		g.setSubGroups(Lists.emptyList());
		ret.put("/project/subgroup", g);

		when(mockBulkQueryService.getGroupAndSubgroups(any())).thenReturn(ret);

		Map<String, DelegatedGroupContents> groupAndSubgroups = dGroupMan.getGroupAndSubgroups("/project",
				"/project");

		assertThat(groupAndSubgroups.size(), is(2));
		assertThat(groupAndSubgroups.get("/project").group.path, is("/project"));
		assertThat(groupAndSubgroups.get("/project/subgroup").group.path, is("/project/subgroup"));
	}

	@Test
	public void shouldGetGroupContents() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		GroupContents con = new GroupContents();
		con.setGroup(new Group("/project/subGroup"));
		con.setSubGroups(Lists.emptyList());
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);

		DelegatedGroupContents contents = dGroupMan.getContents("/project", "/project/subGroup");
		assertThat(contents.group.path, is("/project/subGroup"));
	}

	private void assertException(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}

}
