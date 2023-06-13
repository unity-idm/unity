/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.groups;

import java.util.List;
import java.util.Set;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

/**
 * {@link ChipsWithDropdown} specialization for selecting multiple groups
 * @author K. Benedyczak
 */
public interface GroupsSelection extends Component
{
	static GroupsSelection getGroupsSelection(MessageSource msg, boolean multiSelectable, boolean mandatory)
	{
		return mandatory && !multiSelectable ? new MandatoryGroupSelection(msg) : 
			new OptionalGroupsSelection(msg, multiSelectable);
	}
	
	List<String> getSelectedGroupsWithParents();

	List<String> getSelectedGroupsWithoutParents();

	void setSelectedItems(List<Group> items);
	
	void setReadOnly(boolean readOnly);

	void setItems(List<Group> items);

	void setDescription(String description);

	void setMultiSelectable(boolean multiSelect);
	
	Set<String> getItems();
}
