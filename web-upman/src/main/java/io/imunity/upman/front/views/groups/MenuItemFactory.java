/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.groups;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin.elements.*;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.*;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition.ASIDE;
import static com.vaadin.flow.component.icon.VaadinIcon.*;

class MenuItemFactory
{
	private final GroupService groupService;
	private final MessageSource msg;
	private final Div content;
	private final Runnable viewReloader;

	MenuItemFactory(MessageSource msg, GroupService groupService, Div content, Runnable reloader)
	{
		this.groupService = groupService;
		this.msg = msg;
		this.content = content;
		this.viewReloader = reloader;
	}

	MenuItem createMakePrivateItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.makePrivateAction"), LOCK);
		return new MenuItem(menuButton, event ->
		{
			groupService.setGroupAccessMode(projectGroup, group, false);
			viewReloader.run();
		});
	}

	MenuItem createMakePublicItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.makePublicAction"), UNLOCK);
		return new MenuItem(menuButton, event ->
		{
			groupService.setGroupAccessMode(projectGroup, group, true);
			viewReloader.run();
		});
	}

	MenuItem createAddGroupItem(ProjectGroup projectGroup, Group group, boolean subGroupAvailable)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.addGroupAction"), PLUS_CIRCLE_O);
		return new MenuItem(menuButton, event -> createAddGroupDialog(projectGroup, group, subGroupAvailable).open());
	}

	MenuItem createDeleteGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.deleteGroupAction"), BAN);
		return new MenuItem(menuButton, event -> createConfirmDialog(
				msg.getMessage("RemoveGroupDialog.confirmDelete", group.currentDisplayedName),
				() -> groupService.deleteGroup(projectGroup, group)).open()
		);
	}

	MenuItem createDeleteSubGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.deleteSubprojectGroupAction"), BAN);
		return new MenuItem(menuButton, event -> createConfirmDialog(
				msg.getMessage("RemoveGroupDialog.confirmSubprojectDelete", group.currentDisplayedName),
				() -> groupService.deleteSubProjectGroup(projectGroup, group)).open());
	}

	MenuItem createRenameGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.renameGroupAction"), PENCIL);
		return new MenuItem(menuButton, event -> createRenameDialog(projectGroup, group).open());
	}

	MenuItem createDelegateGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.delegateGroupAction"), WORKPLACE);
		return new MenuItem(menuButton, event -> createDelegateDialog(projectGroup, group).open());
	}

	private Dialog createDelegateDialog(ProjectGroup projectGroup, Group group)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("SubprojectDialog.caption"));

		SubProjectConfigurationLayout subProjectConfigurationLayout = new SubProjectConfigurationLayout(msg, content, group);
		dialog.add(subProjectConfigurationLayout);

		Button saveButton = new SubmitButton(msg::getMessage);
		saveButton.addClickListener(event ->
		{
			groupService.setGroupDelegationConfiguration(
					projectGroup,
					group,
					subProjectConfigurationLayout.enableDelegation.getValue(),
					subProjectConfigurationLayout.enableSubprojects.getValue(),
					subProjectConfigurationLayout.logoUrl.getValue()
			);
			viewReloader.run();
			dialog.close();
		});

		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createAddGroupDialog(ProjectGroup projectGroup, Group group, boolean subGroupAvailable)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("AddGroupDialog.caption"));

		FormLayout dialogLayout = new FormLayout();
		dialogLayout.setWidth("30em");
		LocaleTextFieldDetails localeTextFieldDetails = new LocaleTextFieldDetails(
				msg.getEnabledLocales().values(), msg.getLocale(),
				Optional.of(msg.getMessage("AddGroupDialog.info", group.currentDisplayedName)),
				locale -> ""
		);
		localeTextFieldDetails.focus();

		Checkbox isPublic = new Checkbox(msg.getMessage("AddGroupDialog.public"));
		isPublic.setValue(group.isPublic);
		isPublic.setEnabled(group.isPublic);

		dialogLayout.addFormItem(localeTextFieldDetails, new FormLayoutLabel(msg.getMessage("GroupsComponent.newGroupName")));
		dialogLayout.addFormItem(isPublic, "");
		dialogLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("1em", 1, ASIDE));

		SubProjectConfigurationLayout subProjectConfigurationLayout = new SubProjectConfigurationLayout(msg, content);

		if(!subGroupAvailable)
			subProjectConfigurationLayout.setVisible(false);

		VerticalLayout verticalLayout = new VerticalLayout(dialogLayout, subProjectConfigurationLayout);
		verticalLayout.getStyle().set("gap", "unset");
		dialog.add(verticalLayout);

		Button saveButton = new SubmitButton(msg::getMessage);
		saveButton.addClickListener(event ->
		{
			Map<Locale, String> localeToTxt = localeTextFieldDetails.fields.values().stream()
					.collect(Collectors.toMap(field -> field.locale, TextField::getValue));
			groupService.addGroup(projectGroup, group, localeToTxt, isPublic.getValue());
			if(subProjectConfigurationLayout.enableDelegation.getValue())
				groupService.setGroupDelegationConfiguration(projectGroup, group,
						subProjectConfigurationLayout.enableDelegation.getValue(),
						subProjectConfigurationLayout.enableSubprojects.getValue(),
						subProjectConfigurationLayout.logoUrl.getValue()
				);
			viewReloader.run();
			dialog.close();
		});

		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createConfirmDialog(String txt, Runnable runnable)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("Confirmation"));
		dialog.addClassName(VaadinClassNames.DIALOG_CONFIRM.getName());

		Span label = new Span(txt);

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(label);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = new SubmitButton(msg::getMessage);
		saveButton.addClickListener(event ->
		{
			runnable.run();
			viewReloader.run();
			dialog.close();
		});
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createRenameDialog(ProjectGroup projectGroup, Group group)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("GroupsComponent.renameGroupAction"));

		LocaleTextFieldDetails details = new LocaleTextFieldDetails(
				msg.getEnabledLocales().values(),
				msg.getLocale(),
				Optional.empty(),
				locale -> Optional.ofNullable(group.displayedName.getValueRaw(locale.getLanguage())).orElse("")
		);
		details.focus();

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(details);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = createRenameButton(projectGroup, group, dialog, details.fields.values());
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createBaseDialog(String header)
	{
		return new BaseDialog(header, msg.getMessage("Cancel"), content);
	}

	private Button createRenameButton(ProjectGroup projectGroup, Group group, Dialog dialog, Collection<LocaleTextField> fields)
	{
		Button button = new SubmitButton(msg::getMessage);
		button.addClassName("submit-button");
		button.addClickListener(event ->
		{
			Map<Locale, String> collect = fields.stream().collect(Collectors.toMap(x -> x.locale, TextField::getValue));
			groupService.updateGroupName(projectGroup, group, collect);
			viewReloader.run();
			dialog.close();
		});
		return button;
	}

	static class MenuItem
	{
		Component component;
		ComponentEventListener<ClickEvent<com.vaadin.flow.component.contextmenu.MenuItem>> clickListener;

		private MenuItem(Component component, ComponentEventListener<ClickEvent<com.vaadin.flow.component.contextmenu.MenuItem>> clickListener)
		{
			this.component = component;
			this.clickListener = clickListener;
		}
	}
}
