/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.mvel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mvel2.MVEL;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.group.GroupsChain;
import pl.edu.icm.unity.engine.api.mvel.MVELGroup;

@ExtendWith(MockitoExtension.class)
public class TestMVELGroup
{
	@Test
	public void shouldEncodeGroupPath()
	{
		Group root = new Group("/");
		I18nString dispRoot = new I18nString("Root");
		dispRoot.addValue("en", "enRoot");
		root.setDisplayedName(dispRoot);
		I18nString dispA = new I18nString("GrA");
		dispA.addValue("en", "enA");
		Group a = new Group("/a");
		a.setDisplayedName(dispA);
		I18nString dispB = new I18nString("GrB");
		dispB.addValue("en", "enB");
		Group ab = new Group("/a/b");
		ab.setDisplayedName(dispB);

		Map<String, Group> groupProvider = new HashMap<>();
		groupProvider.put("/", root);
		groupProvider.put("/a", a);
		groupProvider.put("/a/b", ab);
		MVELGroup mvelGroup = new MVELGroup(new GroupsChain(new Group("/a/b").getPathsChain()
				.stream()
				.map(p -> groupProvider.get(p))
				.collect(Collectors.toList())));

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("group", mvelGroup);

		assertThat(
				MVEL.evalToString(
						"disp = def(g) {g.displayedName.getValue(\"en\")}; group.getEncodedGroupPath(':', disp)", map))
				.isEqualTo("enRoot:enA:enB");
	}

	@Test
	public void shouldEncodeGroupPathSkippingFirstElement()
	{
		Group root = new Group("/");
		I18nString dispRoot = new I18nString("Root");
		dispRoot.addValue("en", "enRoot");
		root.setDisplayedName(dispRoot);
		I18nString dispA = new I18nString("GrA");
		dispA.addValue("en", "enA");
		Group a = new Group("/a");
		a.setDisplayedName(dispA);
		I18nString dispB = new I18nString("GrB");
		dispB.addValue("en", "enB");
		Group ab = new Group("/a/b");
		ab.setDisplayedName(dispB);

		Map<String, Group> groupProvider = new HashMap<>();
		groupProvider.put("/", root);
		groupProvider.put("/a", a);
		groupProvider.put("/a/b", ab);
		MVELGroup mvelGroup = new MVELGroup(new GroupsChain(new Group("/a/b").getPathsChain()
				.stream()
				.map(p -> groupProvider.get(p))
				.collect(Collectors.toList())));

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("group", mvelGroup);

		String encoded = MVEL.evalToString(
				"disp = def(g) {g.displayedName.getValue(\"en\")}; group.getEncodedGroupPath(':', 1, disp)", map);
		assertThat(encoded).isEqualTo("enA:enB");
	}

}
