/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import com.vaadin.flow.component.contextmenu.MenuItem;
import io.imunity.upman.av23.front.components.ActionMenu;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

class MemberActionMenu extends ActionMenu
{
	private final MenuItem setProjectRoleItem;
	private final MenuItem setSubProjectRoleItem;

	public MemberActionMenu(MenuItemFactory menuItemFactory,
	                        Supplier<ProjectGroup> selectedProjectGetter,
	                        Supplier<Group> selectedGroupGetter,
	                        Supplier<List<Group>> allGroupsGetter,
	                        Supplier<Set<MemberModel>> selectedMembersGetter)
	{
		MenuItemFactory.MenuItem removeFromProjectItem = menuItemFactory.createRemoveFromProjectItem(selectedProjectGetter, selectedGroupGetter, selectedMembersGetter);
		addItem(removeFromProjectItem.component, removeFromProjectItem.clickListener);

		MenuItemFactory.MenuItem removeFromGroupItem = menuItemFactory.createRemoveFromGroupItem(selectedProjectGetter, selectedGroupGetter, selectedMembersGetter);
		addItem(removeFromGroupItem.component, removeFromGroupItem.clickListener);

		MenuItemFactory.MenuItem addToGroupItem = menuItemFactory.createAddToGroupItem(selectedProjectGetter, allGroupsGetter, selectedMembersGetter);
		addItem(addToGroupItem.component, addToGroupItem.clickListener);

		MenuItemFactory.MenuItem setProjectRoleItem = menuItemFactory.createSetProjectRoleItem(selectedProjectGetter, selectedGroupGetter, selectedMembersGetter);
		this.setProjectRoleItem = addItem(setProjectRoleItem.component, setProjectRoleItem.clickListener);

		MenuItemFactory.MenuItem setSubProjectRoleItem = menuItemFactory.createSetSubProjectRoleItem(selectedProjectGetter, selectedGroupGetter, selectedMembersGetter);
		this.setSubProjectRoleItem = addItem(setSubProjectRoleItem.component, setSubProjectRoleItem.clickListener);

		addOpenedChangeListener(event ->
		{
			boolean anySelected = !selectedMembersGetter.get().isEmpty();
			getItems().forEach(menuItem -> menuItem.setEnabled(anySelected));
		});
	}

	MemberActionMenu switchMode(MembersView.ViewMode viewMode, GroupAuthorizationRole currentUserRole)
	{
		switch (viewMode)
		{
			case PROJECT_MODE:
				switchToProjectMode();
				break;
			case SUBGROUP_MODE:
				switchToRegularSubgroupMode();
				break;
			case SUBPROJECT_MODE:
				switchToSubprojectMode(currentUserRole);
				break;
		}
		return this;
	}

	void switchToSubprojectMode(GroupAuthorizationRole role)
	{
		setProjectRoleItem.setVisible(false);
		setSubProjectRoleItem.setVisible(!role.equals(GroupAuthorizationRole.projectsAdmin));
	}

	void switchToProjectMode()
	{
		setProjectRoleItem.setVisible(true);
		setSubProjectRoleItem.setVisible(false);

	}

	void switchToRegularSubgroupMode()
	{
		setProjectRoleItem.setVisible(false);
		setSubProjectRoleItem.setVisible(false);
	}
}
