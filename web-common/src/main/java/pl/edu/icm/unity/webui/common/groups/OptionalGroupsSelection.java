/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.groups;

import java.util.List;
import java.util.stream.Collectors;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

/**
 * {@link ChipsWithDropdown} specialization for selecting multiple groups
 * @author K. Benedyczak
 */
public class OptionalGroupsSelection extends ChipsWithDropdown<Group> implements GroupsSelection
{
	public OptionalGroupsSelection(UnityMessageSource msg)
	{
		this(msg, true);
	}
	
	public OptionalGroupsSelection(UnityMessageSource msg, boolean multiSelectable)
	{
		super(group -> group.getDisplayedName().getValue(msg), multiSelectable);
	}
	
	@Override
	public List<String> getSelectedGroups()
	{
		return getSelectedItems().stream().map(group -> group.toString()).collect(Collectors.toList());
	}
}
