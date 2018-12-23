/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Group selection related operations.
 * @author P.Piernik
 *
 */
public class GroupSelectionHelper
{
	public static final String GROUPS_TREE_INDENT_CHAR = "\u2003";

	public static int getMinIndent(List<Group> items)
	{
		int min = 100;
		for (Group group : items)
		{
			int indent = StringUtils.countOccurrencesOf(group.toString(), "/");
			if (indent < min)
			{
				min = indent;
			}
		}
		return min;
	}

	public static String generateIndent(int i)
	{
		if (i > 0)
		{
			return String.join("", Collections.nCopies(i, GROUPS_TREE_INDENT_CHAR));
		} else
		{
			return "";
		}
	}

	public static void sort(List<Group> source, Comparator<Group> comparator)
	{
		if (source.isEmpty())
			return;
		Map<String, Group> groupByPath = source.stream().collect(Collectors.toMap(g -> g.toString(), g -> g));

		Group sortRoot = new Group("");
		Map<Group, List<Group>> byParent = source.stream()
				.collect(Collectors.groupingBy(
						g -> groupByPath.get(g.getParentPath()) == null ? sortRoot
								: groupByPath.get(g.getParentPath()),
						Collectors.toList()));

		List<Group> ordered = new ArrayList<>();
		Deque<Group> processor = new LinkedList<>();

		byParent.get(sortRoot).stream().sorted(comparator).forEach(processor::add);

		while (!processor.isEmpty())
		{
			Group tmp = processor.pollLast();
			byParent.getOrDefault(tmp, Collections.emptyList()).stream().sorted(comparator)
					.forEach(processor::add);
			ordered.add(tmp);
		}
		source.clear();
		source.addAll(ordered);
	}

	public static class GroupNameComparator implements Comparator<Group>
	{
		private UnityMessageSource msg;

		public GroupNameComparator(UnityMessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public int compare(Group g1, Group g2)
		{
			return g2.getDisplayedName().getValue(msg).compareTo(g1.getDisplayedName().getValue(msg));
		}

	}
}
