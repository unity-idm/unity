/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.groups;

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
import io.imunity.upman.av23.front.components.GridActionMenu;
import io.imunity.upman.av23.front.components.MenuButton;
import io.imunity.upman.av23.front.components.UnityViewComponent;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.av23.front.views.UpManMenu;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static java.util.stream.Collectors.toSet;

@Route(value = "/groups", layout = UpManMenu.class)
public class GroupsView extends UnityViewComponent
{
	private final MessageSource msg;
	private final ProjectService projectService;
	private final MenuItemFactory menuItemFactory;
	private final TreeGrid<GroupTreeNode> grid;

	private ProjectGroup projectGroup;
	private GroupAuthorizationRole currentUserRole;
	private GroupActionMenuFactory actionMenuFactory;
	private GroupTreeNode root;
	private Set<GroupTreeNode> gridExpandElements;

	public GroupsView(MessageSource msg, ProjectService projectService, GroupService groupService) {
		this.msg = msg;
		this.projectService = projectService;
		this.grid = createGroupsGrid();
		this.menuItemFactory = new MenuItemFactory(msg, groupService, getContent(), this::loadGrid);

		VerticalLayout verticalLayout = new VerticalLayout(createMainContextMenu(), grid);
		verticalLayout.getStyle().set("margin-top", "1em");
		getContent().add(verticalLayout);

		loadData();
	}

	private TreeGrid<GroupTreeNode> createGroupsGrid()
	{
		TreeGrid<GroupTreeNode> grid = new TreeGrid<>();
		grid.addThemeNames("no-border", "no-row-borders");
		grid.addComponentHierarchyColumn(groupTreeNode -> {
			Div div = new Div();
			if(groupTreeNode.isDelegationEnabled())
				div.add(WORKPLACE.create());
			div.add(new Label(groupTreeNode.getDisplayedName()));
			return div;
		});
		grid.addComponentColumn(groupNode -> actionMenuFactory.createMenu(groupNode))
				.setTextAlign(ColumnTextAlign.END);

		grid.addCollapseListener(event -> gridExpandElements.removeAll(event.getItems()));
		grid.addExpandListener(event -> gridExpandElements.addAll(event.getItems()));

		return grid;
	}

	private Component createMainContextMenu()
	{
		GridActionMenu contextMenu = new GridActionMenu();

		contextMenu.addItem(
				new MenuButton(msg.getMessage("GroupsComponent.expandAllAction"), FILE_TREE_SUB),
				event -> grid.expandRecursively(List.of(root), Integer.MAX_VALUE)
		);
		contextMenu.addItem(
				new MenuButton(msg.getMessage("GroupsComponent.collapseAllAction"), FILE_TREE_SMALL),
				event -> grid.collapseRecursively(List.of(root), Integer.MAX_VALUE)
		);

		return contextMenu.getTarget();
	}

	@Override
	public void loadData()
	{
		projectGroup = ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class);
		currentUserRole = projectService.getCurrentUserProjectRole(projectGroup);
		gridExpandElements = new HashSet<>();

		loadGrid();
	}

	private void loadGrid()
	{
		Group projectGroup = projectService.getProjectGroup(this.projectGroup);
		root = projectService.getProjectGroups(projectGroup);

		actionMenuFactory = new GroupActionMenuFactory(menuItemFactory, this.projectGroup, root, currentUserRole);

		grid.setItems(List.of(root), GroupTreeNode::getChildren);
		Set<String> paths = gridExpandElements.stream().map(GroupTreeNode::getPath).collect(toSet());
		grid.expand(root.getAllNodes().stream().filter(node -> paths.contains(node.getPath())).collect(toSet()));
	}
}
