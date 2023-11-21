/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.members;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Route;
import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin.elements.BaseDialog;
import io.imunity.vaadin.elements.MenuButton;
import io.imunity.vaadin.elements.SubmitButton;
import io.imunity.vaadin.elements.CssClassNames;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static java.util.stream.Collectors.toList;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.*;

class MenuItemFactory
{
	private final GroupMembersService groupMembersController;
	private final MessageSource msg;
	private final Div content;
	private final Runnable viewReloader;

	MenuItemFactory(MessageSource msg, GroupMembersService groupMembersController, Div content, Runnable reloader)
	{
		this.groupMembersController = groupMembersController;
		this.msg = msg;
		this.content = content;
		this.viewReloader = reloader;
	}

	MenuItem createRemoveFromProjectItem(Supplier<ProjectGroup> projectGetter,
	                                     Supplier<Set<MemberModel>> modelsGetter, Supplier<GroupAuthorizationRole> roleGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.removeFromProjectAction"), BAN);
		return new MenuItem(menuButton, event -> removeFromProject(projectGetter.get(), roleGetter.get(), modelsGetter.get()));
	}

	MenuItem createRemoveFromGroupItem(Supplier<ProjectGroup> projectGetter, Supplier<Group> groupGetter,
	                                   Supplier<Set<MemberModel>> modelsGetter, Supplier<GroupAuthorizationRole> roleGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.removeFromGroupAction"), FILE_REMOVE);
		return new MenuItem(menuButton, event -> removeFromGroup(projectGetter.get(), groupGetter.get(), roleGetter.get(), modelsGetter.get()));
	}

	MenuItem createAddToGroupItem(Supplier<ProjectGroup> projectGetter, Supplier<List<GroupTreeNode>> groupGetter, Supplier<Set<MemberModel>> modelsGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.addToGroupAction"), PLUS_CIRCLE_O);
		return new MenuItem(menuButton, event -> createAddToGroupDialog(projectGetter.get(), modelsGetter.get(), groupGetter.get()).open());
	}

	MenuItem createSetProjectRoleItem(Supplier<ProjectGroup> projectGetter, Supplier<Group> groupGetter, Supplier<Set<MemberModel>> modelsGetter,
	                                  Supplier<GroupAuthorizationRole> roleGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.setProjectRoleAction"), STAR_O);
		return new MenuItem(menuButton, event -> createSetProjectRoleDialog(projectGetter.get(), groupGetter.get(), modelsGetter.get(), roleGetter.get()).open());
	}

	MenuItem createSetSubProjectRoleItem(Supplier<ProjectGroup> projectGetter, Supplier<Group> groupGetter, Supplier<Set<MemberModel>> modelsGetter,
	                                     Supplier<GroupAuthorizationRole> roleGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.setSubProjectRoleAction"), STAR_O);
		return new MenuItem(menuButton, event -> createSetSubProjectRoleDialog(projectGetter.get(), groupGetter.get(), modelsGetter.get(), roleGetter.get()).open());
	}

	private void removeFromGroup(ProjectGroup projectGroup, Group group, GroupAuthorizationRole role, Set<MemberModel> models)
	{
		if(projectGroup.path.equals(group.path))
		{
			removeFromProject(projectGroup, role, models);
			return;
		}
		if(isCurrentUserSelected(models))
		{
			String message = msg.getMessage("GroupMembersComponent.confirmSelfRemoveFromProject", projectGroup.displayedName);
			createSelfRemoveDialog(
				message, () ->
					{
						groupMembersController.removeFromGroup(projectGroup, group, models);
						viewReloader.run();
					}
			).open();
		}
		else
		{
			groupMembersController.removeFromGroup(projectGroup, group, models);
			viewReloader.run();
		}
	}

	private void removeFromProject(ProjectGroup projectGroup, GroupAuthorizationRole role, Set<MemberModel> models)
	{
		if(isCurrentUserSelected(models))
		{
			String message = msg.getMessage("GroupMembersComponent.confirmSelfRemoveFromProject", projectGroup.displayedName);
			createSelfRemoveDialog(
					message, () ->
					{
						groupMembersController.removeFromProject(projectGroup, models);
						if(role.equals(projectsAdmin))
							reloadMainLayout();
						else
							viewReloader.run();
					}
			).open();
		}
		else
		{
			groupMembersController.removeFromProject(projectGroup, models);
			viewReloader.run();
		}
	}

	private boolean isCurrentUserSelected(Set<MemberModel> models)
	{
		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		return models.stream().map(member -> member.entityId).anyMatch(memberId -> memberId.equals(entityId));
	}

	private void reloadMainLayout()
	{
		UI ui = UI.getCurrent();
		String location = MembersView.class.getAnnotation(Route.class).value();
		ui.getInternals().getRouter().navigate(ui, new Location(location), NavigationTrigger.PROGRAMMATIC);
	}

	private Dialog createSelfRemoveDialog(String txt, Runnable job)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("Confirmation"));
		dialog.addClassName(CssClassNames.DIALOG_CONFIRM.getName());
		dialog.add(new VerticalLayout(new Span(txt)));

		Button saveButton = new SubmitButton(msg::getMessage);
		saveButton.addClickListener(e ->
		{
			job.run();
			dialog.close();
		});
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createAddToGroupDialog(ProjectGroup projectGroup, Set<MemberModel> members, List<GroupTreeNode> groups)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("AddToGroupDialog.caption"));

		ComboBox<GroupTreeNode> groupComboBox = new GroupComboBox(msg);
		groupComboBox.focus();
		groupComboBox.setLabel(msg.getMessage("AddToGroupDialog.info"));
		groupComboBox.setItems(groups);
		if(groups.iterator().hasNext())
			groupComboBox.setValue(groups.iterator().next());
		groupComboBox.getStyle().set("width", "24em");

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
		dialogLayout.add(new Span(msg.getMessage("AddToGroupDialog.selectGroup")), groupComboBox);
		dialog.add(dialogLayout);

		Button saveButton = createAddToGroupButton(projectGroup, dialog, groupComboBox, members);
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createSetProjectRoleDialog(ProjectGroup projectGroup, Group group, Set<MemberModel> items, GroupAuthorizationRole role)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("RoleSelectionDialog.projectCaption"));

		RadioButtonGroup<GroupAuthorizationRole> radioGroup = createRoleRadioButtonGroup(role, items);
		Span label = new Span(msg.getMessage("RoleSelectionDialog.projectRole"));

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(label, radioGroup);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = createSetProjectRoleButton(projectGroup, group, dialog, radioGroup, items);
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private RadioButtonGroup<GroupAuthorizationRole> createRoleRadioButtonGroup(GroupAuthorizationRole role, Set<MemberModel> items)
	{
		RadioButtonGroup<GroupAuthorizationRole> radioGroup = new RadioButtonGroup<>();
		radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		List<GroupAuthorizationRole> roles;
		if(role.equals(manager))
			roles = List.of(manager, regular);
		else
			roles = List.of(projectsAdmin, manager, regular);

		radioGroup.setItems(roles);
		radioGroup.setItemLabelGenerator(r-> msg.getMessage("Role." + r.toString().toLowerCase()));
		if(items.size() == 1)
			radioGroup.setValue(GroupAuthorizationRole.valueOf(items.iterator().next().role.getKey()));
		return radioGroup;
	}

	private Dialog createSetSubProjectRoleDialog(ProjectGroup projectGroup, Group group, Set<MemberModel> items, GroupAuthorizationRole role)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("RoleSelectionDialog.subprojectCaption"));

		RadioButtonGroup<GroupAuthorizationRole> radioGroup = createRoleRadioButtonGroup(role, items);
		Span label = new Span(msg.getMessage("RoleSelectionDialog.subprojectRole"));

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(label, radioGroup);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = createSetSubProjectRoleButton(projectGroup, group, dialog, radioGroup, items);
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createBaseDialog(String header)
	{
		return new BaseDialog(header, msg.getMessage("Cancel"), content);
	}

	private Button createSetSubProjectRoleButton(ProjectGroup projectGroup, Group group, Dialog dialog, RadioButtonGroup<GroupAuthorizationRole> radioGroup, Set<MemberModel> items)
	{
		Button button = new SubmitButton(msg::getMessage);
		button.addClickListener(event ->
		{
			groupMembersController.updateRole(projectGroup, group, radioGroup.getValue(), items);
			viewReloader.run();
			dialog.close();
		});
		return button;
	}

	private Button createSetProjectRoleButton(ProjectGroup projectGroup, Group group, Dialog dialog, RadioButtonGroup<GroupAuthorizationRole> radioGroup, Set<MemberModel> items)
	{
		Button button = new SubmitButton(msg::getMessage);
		button.addClickListener(event ->
		{
			GroupAuthorizationRole role = radioGroup.getValue();
			if(role.equals(regular) && isCurrentUserSelected(items))
			{
				dialog.close();
				createSelfRemoveDialog(
						msg.getMessage("GroupMembersComponent.confirmSelfRevokeManagerPrivileges", projectGroup.displayedName),
						() ->
						{
							groupMembersController.updateRole(projectGroup, group, role, items);
							viewReloader.run();
						}
				).open();
				return;
			}
			groupMembersController.updateRole(projectGroup, group, role, items);
			viewReloader.run();
			dialog.close();
		});
		return button;
	}

	private Button createAddToGroupButton(ProjectGroup projectGroup, Dialog dialog, ComboBox<GroupTreeNode> comboBox, Set<MemberModel> members)
	{
		Button button = new SubmitButton(msg::getMessage);
		button.addClickListener(event ->
		{
			GroupTreeNode value = comboBox.getValue();
			List<GroupTreeNode> parents = value.getNodeWithAllOffspring();
			parents.add(value);

			groupMembersController.addToGroup(projectGroup, parents.stream().map(node -> node.group).collect(toList()), members);
			dialog.close();
		});
		return button;
	}

	static class MenuItem
	{
		MenuButton component;
		ComponentEventListener<ClickEvent<com.vaadin.flow.component.contextmenu.MenuItem>> clickListener;

		private MenuItem(MenuButton component, ComponentEventListener<ClickEvent<com.vaadin.flow.component.contextmenu.MenuItem>> clickListener)
		{
			this.component = component;
			this.clickListener = clickListener;
		}
	}
}
