/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.groups;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;
import io.imunity.upman.front.UpmanViewComponent;
import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.front.views.UpManMenu;
import io.imunity.upman.utils.ProjectService;
import io.imunity.vaadin.elements.ActionMenu;
import io.imunity.vaadin.elements.MenuButton;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

import javax.annotation.security.PermitAll;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static java.util.stream.Collectors.toSet;

@PermitAll
@Route(value = "/groups", layout = UpManMenu.class)
public class GroupsView extends UpmanViewComponent
{
	private final MessageSource msg;
	private final ProjectService projectService;
	private final MenuItemFactory menuItemFactory;
	private final TreeGrid<GroupTreeNode> grid;

	private ProjectGroup projectGroup;
	private GroupAuthorizationRole currentUserRole;
	private GroupActionMenuFactory actionMenuFactory;
	private GroupTreeNode root;
	private Set<GroupTreeNode> gridExpandedElements;

	public GroupsView(MessageSource msg, ProjectService projectService, GroupService groupService)
	{
		this.msg = msg;
		this.projectService = projectService;
		this.grid = createGroupsGrid();
		this.menuItemFactory = new MenuItemFactory(msg, groupService, getContent(), this::loadGrid);

		VerticalLayout verticalLayout = new VerticalLayout(createMainContextMenu(), grid);
		verticalLayout.getStyle().set("margin-top", "1em");
		getContent().add(verticalLayout);
	}

	private TreeGrid<GroupTreeNode> createGroupsGrid()
	{
		TreeGrid<GroupTreeNode> grid = new TreeGrid<>();
		grid.addThemeNames("no-border", "no-row-borders");
		grid.addComponentHierarchyColumn(groupTreeNode -> {
			Div div = new Div();
			if(groupTreeNode.isDelegationEnabled())
				div.add(WORKPLACE.create());
			if(groupTreeNode.isPublic())
				div.add(UNLOCK.create());
			div.add(new Label(groupTreeNode.getDisplayedName()));
			div.addClickListener(event ->
			{
				if(gridExpandedElements.contains(groupTreeNode))
					grid.collapseRecursively(Stream.of(groupTreeNode), Integer.MAX_VALUE);
				else
					grid.expand(groupTreeNode);
			});
			return div;
		});
		grid.addComponentColumn(groupNode -> actionMenuFactory.createMenu(groupNode))
				.setTextAlign(ColumnTextAlign.END);

		grid.addCollapseListener(event ->
		{
			gridExpandedElements.removeAll(event.getItems());
			grid.collapseRecursively(event.getItems(), Integer.MAX_VALUE);
		});
		grid.addExpandListener(event -> gridExpandedElements.addAll(event.getItems()));

		return grid;
	}

	private Component createMainContextMenu()
	{
		ActionMenu contextMenu = new ActionMenu();

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
		if(projectGroup == null)
			return;
		currentUserRole = projectService.getCurrentUserProjectRole(projectGroup);
		gridExpandedElements = new HashSet<>();

		loadGrid();
	}

	private void loadGrid()
	{
		Set<String> paths = getExtendedGroupPaths();
		root = projectService.getProjectGroups(projectGroup);

		actionMenuFactory = new GroupActionMenuFactory(menuItemFactory, this.projectGroup, root, currentUserRole);

		gridExpandedElements.clear();
		grid.setItems(List.of(root), GroupTreeNode::getChildren);
		grid.expand(root.getNodeWithAllOffspring().stream().filter(node -> paths.contains(node.getPath())).collect(toSet()));
		grid.expand(root);
	}

	private Set<String> getExtendedGroupPaths()
	{
		Set<String> paths;
		if(root == null)
			paths = Set.of();
		else
			paths = root.getNodeWithAllOffspring().stream()
				.filter(grid::isExpanded)
				.map(GroupTreeNode::getPath)
			.collect(Collectors.toSet());
		return paths;
	}
}
