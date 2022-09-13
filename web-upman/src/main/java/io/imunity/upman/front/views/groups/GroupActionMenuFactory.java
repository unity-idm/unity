/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.groups;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.MenuItem;
import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin23.elements.ActionMenu;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

class GroupActionMenuFactory
{
	private final MenuItemFactory menuItemFactory;
	private final ProjectGroup projectGroup;
	private final GroupTreeNode root;
	private final GroupAuthorizationRole role;

	public GroupActionMenuFactory(MenuItemFactory menuItemFactory,
	                              ProjectGroup projectGroup,
	                              GroupTreeNode root,
	                              GroupAuthorizationRole role)
	{
		this.menuItemFactory = menuItemFactory;
		this.root = root;
		this.projectGroup = projectGroup;
		this.role = role;
	}

	public Component createMenu(GroupTreeNode groupNode)
	{
		ActionMenu menu = new ActionMenu();
		boolean subGroupAvailable = checkIfAdminCanCreateSubproject();

		createDeleteItem(groupNode, menu);
		createAddItem(groupNode, menu, subGroupAvailable);
		MenuItem makePrivateMenuItem = createMakePrivateItem(groupNode, menu);
		MenuItem makePublicMenuItem = createMakePublicItem(groupNode, menu);
		createRenameItem(groupNode, menu);
		crateDelegateItem(groupNode, menu);

		menu.addOpenedChangeListener(event ->
		{
			if(isNodePublicAndNodesChildrenNotPublic(groupNode))
			{
				makePrivateMenuItem.setVisible(true);
				makePublicMenuItem.setVisible(false);
			}
			if(isParentsNodePublicAndNodeNotPublic(groupNode))
			{
				makePrivateMenuItem.setVisible(false);
				makePublicMenuItem.setVisible(true);
			}
		});

		return menu.getTarget();
	}

	private void crateDelegateItem(GroupTreeNode groupNode, ActionMenu menu)
	{
		if(checkIfAdminCanChangeDelegationConfig(groupNode))
		{
			MenuItemFactory.MenuItem delegateItem = menuItemFactory.createDelegateGroupItem(projectGroup, groupNode.group);
			menu.addItem(delegateItem.component, delegateItem.clickListener);
		}
	}

	private void createRenameItem(GroupTreeNode groupNode, ActionMenu menu)
	{
		if(!groupNode.isRoot())
		{
			MenuItemFactory.MenuItem renameItem = menuItemFactory.createRenameGroupItem(projectGroup, groupNode.group);
			menu.addItem(renameItem.component, renameItem.clickListener);
		}
	}

	private MenuItem createMakePublicItem(GroupTreeNode groupNode, ActionMenu menu)
	{
		MenuItemFactory.MenuItem makePublicItem = menuItemFactory.createMakePublicItem(projectGroup, groupNode.group);
		MenuItem menuItem = menu.addItem(makePublicItem.component, makePublicItem.clickListener);
		menuItem.setVisible(false);
		return menuItem;
	}

	private MenuItem createMakePrivateItem(GroupTreeNode groupNode, ActionMenu menu)
	{
		MenuItemFactory.MenuItem makePrivateItem = menuItemFactory.createMakePrivateItem(projectGroup, groupNode.group);
		MenuItem menuItem = menu.addItem(makePrivateItem.component, makePrivateItem.clickListener);
		menuItem.setVisible(false);
		return menuItem;
	}

	private void createAddItem(GroupTreeNode groupNode, ActionMenu menu, boolean subGroupAvailable)
	{
		MenuItemFactory.MenuItem addGroupItem = menuItemFactory.createAddGroupItem(projectGroup, groupNode.group, subGroupAvailable);
		menu.addItem(addGroupItem.component, addGroupItem.clickListener);
	}

	private void createDeleteItem(GroupTreeNode groupNode, ActionMenu menu)
	{
		if(!groupNode.isRoot() && !groupNode.isDelegationEnabled())
		{
			MenuItemFactory.MenuItem deleteGroupItem = menuItemFactory.createDeleteGroupItem(projectGroup, groupNode.group);
			menu.addItem(deleteGroupItem.component, deleteGroupItem.clickListener);
		}

		if(checkIfAdminCanChangeDelegationConfig(groupNode) && groupNode.isDelegationEnabled())
		{
			MenuItemFactory.MenuItem deleteSubGroupItem = menuItemFactory.createDeleteSubGroupItem(projectGroup, groupNode.group);
			menu.addItem(deleteSubGroupItem.component, deleteSubGroupItem.clickListener);
		}
	}

	private boolean isParentsNodePublicAndNodeNotPublic(GroupTreeNode groupNode)
	{
		return groupNode.parent.map(GroupTreeNode::isPublic).orElse(true) && !groupNode.isPublic();
	}

	private boolean isNodePublicAndNodesChildrenNotPublic(GroupTreeNode groupNode)
	{
		return groupNode.getChildren().stream().noneMatch(GroupTreeNode::isPublic) && groupNode.isPublic();
	}

	private boolean checkIfAdminCanChangeDelegationConfig(GroupTreeNode group)
	{
		if (group.isRoot())
			return false;
		return checkIfAdminCanCreateSubproject();
	}

	private boolean checkIfAdminCanCreateSubproject()
	{
		if (!root.group.delegationEnabled || !root.group.subprojectsDelegationEnabled)
			return false;
		return role.equals(GroupAuthorizationRole.projectsAdmin);
	}

}
