/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import io.imunity.upman.av23.components.ProjectService;
import io.imunity.upman.av23.front.components.SearchField;
import io.imunity.upman.av23.front.components.UnityViewComponent;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.av23.front.views.UpManMenu;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Route(value = "/members", layout = UpManMenu.class)
@RouteAlias(value = "/", layout = UpManMenu.class)
public class MembersView extends UnityViewComponent
{
	enum ViewMode {PROJECT_MODE, SUBPROJECT_MODE, SUBGROUP_MODE}

	private final ProjectService projectService;
	private final GroupMembersService groupMembersService;
	private final MessageSource msg;

	private final ComboBox<Group> groupsComboBox;
	private final MemberActionMenu mainContextMenu;
	private final TextField searchField;
	private final MenuItemFactory menuItemFactory;

	private MembersGrid grid;

	private ViewMode mode;
	private GroupAuthorizationRole currentUserRole;
	private ProjectGroup projectGroup;
	private List<Group> groups;

	public MembersView(MessageSource msg, ProjectService projectService, GroupMembersService groupMembersService)
	{
		this.msg = msg;
		this.menuItemFactory = new MenuItemFactory(msg, groupMembersService, getContent(), this::reload);
		this.projectService = projectService;
		this.groupMembersService = groupMembersService;

		groupsComboBox = createGroupComboBox();

		HorizontalLayout groupComboBoxLayout = createGroupComboBoxLayout(msg);

		searchField = createSearchField();

		mainContextMenu = createContextMenu(() -> grid.getSelectedItems());

		HorizontalLayout menuAndSearchLayout = createMenuAndSearchLayout(mainContextMenu.getTarget(), searchField);

		getContent().add(groupComboBoxLayout, menuAndSearchLayout);
		loadData();
	}

	private TextField createSearchField()
	{
		return new SearchField(msg.getMessage("GroupMembersComponent.search"), this::reload);
	}

	private HorizontalLayout createMenuAndSearchLayout(Component memberActionMenu, TextField textField)
	{
		HorizontalLayout layout = new HorizontalLayout(memberActionMenu, textField);
		layout.setAlignItems(FlexComponent.Alignment.END);
		layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		layout.getStyle().set("padding-left", "1em");
		return layout;
	}

	private HorizontalLayout createGroupComboBoxLayout(MessageSource msg)
	{
		HorizontalLayout layout = new HorizontalLayout(new Label(msg.getMessage("GroupMemberView.subGroupComboCaption")), groupsComboBox);
		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		layout.getStyle().set("margin-top", "2em");
		return layout;
	}

	private GroupComboBox createGroupComboBox()
	{
		GroupComboBox groupComboBox = new GroupComboBox();
		groupComboBox.getStyle().set("width", "50%");
		groupComboBox.addValueChangeListener(event ->
		{
			if(event.getValue() != null)
			{
				loadGridContent(event.getValue());
				switchViewMode(event.getValue());
			}
		});
		return groupComboBox;
	}

	private MembersGrid createMembersGrid(Map<String, String> attributes)
	{
		return new MembersGrid(attributes, msg, this::createGridRowContextMenu);
	}

	private Component createGridRowContextMenu(MemberModel model)
	{
		return createContextMenu(() -> Set.of(model))
				.switchMode(mode, currentUserRole)
				.getTarget();
	}

	private MemberActionMenu createContextMenu(Supplier<Set<MemberModel>> selectedMembersGetter)
	{
		return new MemberActionMenu(
				menuItemFactory,
				() -> projectGroup,
				groupsComboBox::getValue,
				() -> groups,
				selectedMembersGetter
		);
	}

	private void reload()
	{
		loadGridContent(groupsComboBox.getValue());
	}

	@Override
	public void loadData()
	{
		projectGroup = ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class);
		currentUserRole = projectService.getCurrentUserProjectRole(projectGroup);

		GroupTreeNode groupTreeNode = projectService.getProjectGroups(projectGroup);

		groups = groupTreeNode.getAllElements();
		groupsComboBox.setItems(groups);
		if (groups.iterator().hasNext())
			groupsComboBox.setValue(groups.iterator().next());
	}

	private void switchViewMode(Group group)
	{
		if (this.projectGroup.path.equals(group.path))
			switchViewToProjectMode();
		else if (group.delegationEnabled)
			switchViewToSubprojectMode();
		else
			switchViewToRegularSubgroupMode();
	}

	private void loadGridContent(Group selectedGroup)
	{
		if(grid == null)
		{
			grid = createMembersGrid(groupMembersService.getAdditionalAttributeNamesForProject(projectGroup));
			getContent().add(grid);
		}
		List<MemberModel> members = groupMembersService.getGroupMembers(projectGroup, selectedGroup).stream()
				.filter(member -> member.anyFieldContains(searchField.getValue()))
				.collect(Collectors.toList());
		grid.setItems(members);
	}

	void switchViewToSubprojectMode()
	{
		mode = ViewMode.SUBPROJECT_MODE;
		grid.switchToSubprojectMode();
		mainContextMenu.switchToSubprojectMode(currentUserRole);
	}

	void switchViewToProjectMode()
	{
		mode = ViewMode.PROJECT_MODE;
		grid.switchToProjectMode();
		mainContextMenu.switchToProjectMode();
	}

	void switchViewToRegularSubgroupMode()
	{
		mode = ViewMode.SUBGROUP_MODE;
		grid.switchVToRegularSubgroupMode();
		mainContextMenu.switchToRegularSubgroupMode();
	}

}
