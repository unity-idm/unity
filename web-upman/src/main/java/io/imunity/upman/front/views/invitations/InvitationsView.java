/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.imunity.upman.front.components.*;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.front.views.UpManMenu;
import io.imunity.upman.utils.ProjectService;
import pl.edu.icm.unity.MessageSource;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.ENVELOPE;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;

@Route(value = "/invitations", layout = UpManMenu.class)
public class InvitationsView extends UnityViewComponent
{
	private final ProjectService projectService;
	private final InvitationsService invitationsService;
	private final MessageSource msg;

	private final Grid<InvitationModel> grid;
	private final TextField searchField;
	private ProjectGroup projectGroup;

	public InvitationsView(MessageSource msg, InvitationsService invitationsService, ProjectService projectService)
	{
		this.projectService = projectService;
		this.invitationsService = invitationsService;
		this.msg = msg;


		Button invitationButton = createInvitationButton();
		searchField = createSearchField();
		grid = createGrid();

		getContent().add(
				createInvitationButtonAndSearchLayout(invitationButton, searchField),
				createMainMenuLayout(),
				grid
		);

		loadData();
	}

	private HorizontalLayout createMainMenuLayout()
	{
		HorizontalLayout horizontalLayout = new HorizontalLayout(createContextMenu(grid::getSelectedItems));
		horizontalLayout.getStyle().set("margin-left", "1.2em");
		return horizontalLayout;
	}

	private HorizontalLayout createInvitationButtonAndSearchLayout(Button invitationButton, TextField searchField)
	{
		HorizontalLayout layout = new HorizontalLayout(invitationButton, searchField);
		layout.setAlignItems(FlexComponent.Alignment.END);
		layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		layout.getStyle().set("margin-top", "1em");
		layout.getStyle().set("margin-bottom", "1em");
		return layout;
	}

	private Button createInvitationButton()
	{
		Button invitationButton = new Button(msg.getMessage("Invitations.newInvite"));
		invitationButton.addClickListener(event -> createInvitationDialog().open());

		return invitationButton;
	}

	private Dialog createInvitationDialog()
	{
		Dialog dialog = new BaseDialog(msg.getMessage("NewInvitationDialog.caption"), msg.getMessage("Cancel"), getContent());
		InvitationForm invitationForm = new InvitationForm(msg, projectGroup, projectService.getProjectGroups(projectGroup).getAllOffspring(), getContent());
		dialog.add(invitationForm);

		Button saveButton = new SubmitButton(msg);
		saveButton.addClickListener(event ->
		{
			if(invitationForm.isValid())
			{
				invitationsService.addInvitations(invitationForm.getInvitationRequest());
				dialog.close();
				loadData();
			}
		});

		dialog.getFooter().add(saveButton);
		return dialog;
	}

	private Grid<InvitationModel> createGrid()
	{
		return new InvitationGrid(msg, this::createGridContextMenu, getContent());
	}

	private TextField createSearchField()
	{
		return new SearchField(msg.getMessage("Search"), this::loadData);
	}

	private Component createGridContextMenu(InvitationModel model)
	{
		return createContextMenu(() -> Set.of(model));
	}

	private Component createContextMenu(Supplier<Set<InvitationModel>> invitationsGetter)
	{
		ActionMenu menu = new ActionMenu();

		menu.addItem(
				new MenuButton(msg.getMessage("InvitationsComponent.removeInvitationAction"), TRASH),
				event ->
				{
					invitationsService.removeInvitations(projectGroup, invitationsGetter.get());
					loadData();
				}
		);
		menu.addItem(
				new MenuButton(msg.getMessage("InvitationsComponent.resendInvitationAction"), ENVELOPE),
				event ->
				{
					invitationsService.resendInvitations(projectGroup, invitationsGetter.get());
					loadData();
				}
		);

		return menu.getTarget();
	}

	@Override
	public void loadData()
	{
		projectGroup = ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class);

		List<InvitationModel> invitations = invitationsService.getInvitations(projectGroup).stream()
				.filter(model -> model.anyFieldContains(searchField.getValue()))
				.collect(Collectors.toList());
		grid.setItems(invitations);
	}
}
