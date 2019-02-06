/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.registration;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;

public class TestGroupDiffUtils
{
	@Test
	public void removedShouldNotContainRootGroup()
	{

		GroupRegistrationParam param = new GroupRegistrationParam();
		param.setGroupPath("/**");

		RequestedGroupDiff diff = GroupDiffUtils.getSingleGroupDiff(getAllGroups(), getUsersGroup(),
				new GroupSelection(Lists.newArrayList()), param);

		assertThat(diff.remain.size(), is(1));
		assertThat(diff.remain, hasItems("/"));
		assertThat(diff.toRemove, hasItems("/A", "/A/B"));
		assertThat(diff.toRemove, not(hasItems("/")));
	}

	@Test
	public void shouldGenerateCorrectSingleDiff()
	{

		GroupRegistrationParam param = new GroupRegistrationParam();
		param.setGroupPath("/A/**");

		RequestedGroupDiff diff = GroupDiffUtils.getSingleGroupDiff(getAllGroups() ,getUsersGroup(),
				new GroupSelection(Lists.newArrayList("/A", "/A/AA")), param);

		assertThat(diff.remain.size(), is(1));
		assertThat(diff.remain, hasItems("/A"));

		assertThat(diff.toRemove.size(), is(1));
		assertThat(diff.toRemove, hasItems("/A/B"));

		assertThat(diff.toAdd.size(), is(1));
		assertThat(diff.toAdd, hasItems("/A/AA"));
	}

	@Test
	public void shouldGenerateCorrectAllParamsGroupDiff()
	{

		GroupRegistrationParam param1 = new GroupRegistrationParam();
		param1.setGroupPath("/A/**");

		GroupRegistrationParam param2 = new GroupRegistrationParam();
		param2.setGroupPath("/B/**");

		List<GroupSelection> groupSelections = new ArrayList<>();
		groupSelections.add(new GroupSelection(Lists.newArrayList("/A", "/A/AB")));
		groupSelections.add(new GroupSelection(Lists.newArrayList("/B", "/B/BB")));

		List<Group> userGroups = new ArrayList<>();
		userGroups.addAll(getUsersGroup());
		userGroups.add(new Group("/B"));
		userGroups.add(new Group("/B/CC"));

		RequestedGroupDiff diff = GroupDiffUtils.getAllRequestedGroupsDiff(getAllGroups(),userGroups, groupSelections,
				Arrays.asList(param1, param2));

		assertThat(diff.remain.size(), is(2));
		assertThat(diff.remain, hasItems("/A", "/B"));

		assertThat(diff.toRemove.size(), is(2));
		assertThat(diff.toRemove, hasItems("/A/B", "/B/CC"));

		assertThat(diff.toAdd.size(), is(2));
		assertThat(diff.toAdd, hasItems("/A/AB", "/B/BB"));
	}

	private List<Group> getUsersGroup()
	{
		return Arrays.asList(new Group("/"), new Group("/A"), new Group("/A/B"));
	}
	
	private List<Group> getAllGroups()
	{
		return Arrays.asList(new Group("/"), new Group("/A"), new Group("/B"), new Group("/A/B"), new Group("/A/AA"),
				new Group("/A/AB"), new Group("/B/BB"), new Group("/B/CC"));
	}
}
