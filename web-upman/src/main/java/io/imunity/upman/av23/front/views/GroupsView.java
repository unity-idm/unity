/*
 * Copyright (c) 2020 Bixbit s.c. All rights reserved.
 * See LICENSE file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;
import io.imunity.upman.av23.components.ProjectService;
import io.imunity.upman.av23.front.components.UnityViewComponent;
import io.imunity.upman.av23.front.components.GridActionMenu;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import io.imunity.upman.av23.front.components.MenuButton;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.views.members.GroupMembersService;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;

import java.util.List;

import static com.vaadin.flow.component.icon.VaadinIcon.*;

@Route(value = "/groups", layout = UpManMenu.class)
public class GroupsView extends UnityViewComponent
{
	private final MessageSource msg;
	private final ProjectService projectController;
	private final GroupMembersService groupMembersController;
	private final TreeGrid<GroupTreeNode> grid;

	GroupTreeNode groupTreeNode;

	@Autowired
	public GroupsView(MessageSource msg, ProjectService projectController, GroupMembersService groupMembersController) {
		this.msg = msg;
		this.projectController = projectController;
		this.groupMembersController = groupMembersController;
		this.grid = new TreeGrid<>();

		grid.addThemeNames("no-border", "no-row-borders");
		grid.addComponentHierarchyColumn(x -> {
			Div div = new Div();
			if(x.isBaseLevel())
				div.add(WORKPLACE.create());
			div.add(new Label(x.getDisplayedName()));
			return div;
		});
		grid.addComponentColumn(x -> createContextMenu())
				.setTextAlign(ColumnTextAlign.END);

		VerticalLayout verticalLayout = new VerticalLayout(createMainContextMenu(), grid);
		verticalLayout.getStyle().set("margin-top", "1em");
		getContent().add(verticalLayout);

		loadData();
	}

	private Component createMainContextMenu()
	{
		GridActionMenu contextMenu = new GridActionMenu();

		contextMenu.addItem(
				new MenuButton(msg.getMessage("GroupsComponent.expandAllAction"), FILE_TREE_SUB),
				event -> grid.expandRecursively(List.of(groupTreeNode), Integer.MAX_VALUE)
		);
		contextMenu.addItem(
				new MenuButton(msg.getMessage("GroupsComponent.collapseAllAction"), FILE_TREE_SMALL),
				event -> grid.collapseRecursively(List.of(groupTreeNode), Integer.MAX_VALUE)
		);

		return contextMenu.getTarget();
	}

	private Component createContextMenu()
	{
		GridActionMenu contextMenu = new GridActionMenu();

		return contextMenu.getTarget();
	}

	@Override
	public void loadData()
	{
		ProjectGroup project= ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class);
		Group projectGroup = projectController.getProjectGroup(project);
		groupTreeNode = groupMembersController.getProjectGroups(projectGroup);

		grid.setItems(List.of(groupTreeNode), GroupTreeNode::getChildren);
	}
}
