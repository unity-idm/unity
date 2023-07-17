/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.registration;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;

public class TestGroupDiffUtils
{
	@Test
	public void removedShouldNotContainRootGroup()
	{

		GroupRegistrationParam param = new GroupRegistrationParam();
		param.setGroupPath("/**");

		RequestedGroupDiff diff = GroupDiffUtils.getSingleGroupDiff(getAllGroups(), getUsersGroup(),
				new GroupSelection(Lists.newArrayList()), param);

		assertThat(diff.remain).hasSize(1);
		assertThat(diff.remain).contains("/");
		assertThat(diff.toRemove).contains("/A", "/A/B");
		assertThat(diff.toRemove).doesNotContain("/");
	}

	@Test
	public void shouldGenerateCorrectSingleDiff()
	{

		GroupRegistrationParam param = new GroupRegistrationParam();
		param.setGroupPath("/A/**");

		RequestedGroupDiff diff = GroupDiffUtils.getSingleGroupDiff(getAllGroups() ,getUsersGroup(),
				new GroupSelection(Lists.newArrayList("/A", "/A/AA")), param);

		assertThat(diff.remain).hasSize(1);
		assertThat(diff.remain).contains("/A");

		assertThat(diff.toRemove).hasSize(1);
		assertThat(diff.toRemove).contains("/A/B");

		assertThat(diff.toAdd).hasSize(1);
		assertThat(diff.toAdd).contains("/A/AA");
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

		assertThat(diff.remain).hasSize(2);
		assertThat(diff.remain).contains("/A", "/B");

		assertThat(diff.toRemove).hasSize(2);
		assertThat(diff.toRemove).contains("/A/B", "/B/CC");

		assertThat(diff.toAdd).hasSize(2);
		assertThat(diff.toAdd).contains("/A/AB", "/B/BB");
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
