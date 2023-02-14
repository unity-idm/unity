/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.forms.groups;

import pl.edu.icm.unity.types.basic.Group;

import java.util.List;
import java.util.Set;

public interface GroupsSelection
{
	List<String> getSelectedGroupsWithParents();

	List<String> getSelectedGroupsWithoutParents();

	void setSelectedItems(List<Group> items);
	
	void setReadOnly(boolean readOnly);

	void setItems(List<Group> items);

	void setDescription(String description);

	void setMultiSelectable(boolean multiSelect);
	
	Set<String> getItems();
}
