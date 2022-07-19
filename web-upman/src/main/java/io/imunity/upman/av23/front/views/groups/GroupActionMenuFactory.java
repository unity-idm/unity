/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.groups;

import com.vaadin.flow.component.Component;
import io.imunity.upman.av23.front.components.ActionMenu;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import io.imunity.upman.av23.front.model.ProjectGroup;
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
		createMakePrivateItem(groupNode, menu);
		createMakePublicItem(groupNode, menu);
		createRenameItem(groupNode, menu);
		crateDelegateItem(groupNode, menu);

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

	private void createMakePublicItem(GroupTreeNode groupNode, ActionMenu menu)
	{
		if(isParentsNodePublicAndNodeNotPublic(groupNode))
		{
			MenuItemFactory.MenuItem makePublicItem = menuItemFactory.createMakePublicItem(projectGroup, groupNode.group);
			menu.addItem(makePublicItem.component, makePublicItem.clickListener);
		}
	}

	private void createMakePrivateItem(GroupTreeNode groupNode, ActionMenu menu)
	{
		if(isNodePublicAndNodesChildrenNotPublic(groupNode))
		{
			MenuItemFactory.MenuItem makePrivateItem = menuItemFactory.createMakePrivateItem(projectGroup, groupNode.group);
			menu.addItem(makePrivateItem.component, makePrivateItem.clickListener);
		}
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
		if (!root.group.delegationEnabled || !root.group.delegationEnableSubprojects)
			return false;
		return role.equals(GroupAuthorizationRole.projectsAdmin);
	}

}
