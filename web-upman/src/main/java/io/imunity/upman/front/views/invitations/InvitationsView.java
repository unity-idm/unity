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
import io.imunity.upman.front.UpmanViewComponent;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.front.views.UpManMenu;
import io.imunity.upman.utils.ProjectService;
import io.imunity.vaadin.elements.*;
import pl.edu.icm.unity.base.message.MessageSource;

import javax.annotation.security.PermitAll;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.ENVELOPE;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;

@PermitAll
@Breadcrumb(key = "UpManMenu.invitations")
@Route(value = "/invitations", layout = UpManMenu.class)
public class InvitationsView extends UpmanViewComponent
{
	private final ProjectService projectService;
	private final InvitationsService invitationsService;
	private final MessageSource msg;

	private final Grid<InvitationModel> grid;
	private final TextField searchField;
	private final Button invitationButton;
	private ProjectGroup projectGroup;

	public InvitationsView(MessageSource msg, InvitationsService invitationsService, ProjectService projectService)
	{
		this.projectService = projectService;
		this.invitationsService = invitationsService;
		this.msg = msg;


		invitationButton = createInvitationButton();
		searchField = createSearchField();
		grid = createGrid();

		getContent().add(
				createInvitationButtonAndSearchLayout(invitationButton, searchField),
				createMainMenuLayout(),
				grid
		);
	}

	private HorizontalLayout createMainMenuLayout()
	{
		HorizontalLayout horizontalLayout = new HorizontalLayout(createContextMenu(grid::getSelectedItems));
		horizontalLayout.getStyle().set("margin-left", "1.3em");
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

		Button saveButton = new SubmitButton(msg::getMessage);
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

		MenuButton removeInvitationButton = new MenuButton(msg.getMessage("InvitationsComponent.removeInvitationAction"), TRASH);
		menu.addItem(
				removeInvitationButton,
				event ->
				{
					invitationsService.removeInvitations(projectGroup, invitationsGetter.get());
					loadData();
				}
		);
		MenuButton resendInvitationButton = new MenuButton(msg.getMessage("InvitationsComponent.resendInvitationAction"), ENVELOPE);
		menu.addItem(
				resendInvitationButton,
				event ->
				{
					invitationsService.resendInvitations(projectGroup, invitationsGetter.get());
					loadData();
				}
		);

		menu.addOpenedChangeListener(event ->
		{
			boolean anySelected = !invitationsGetter.get().isEmpty();
			menu.getItems().forEach(menuItem -> menuItem.setEnabled(anySelected));
			removeInvitationButton.setEnabled(anySelected);
			resendInvitationButton.setEnabled(anySelected);
		});

		return menu.getTarget();
	}

	@Override
	public void loadData()
	{
		projectGroup = ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class);

		invitationButton.setVisible(
				(projectGroup.registrationForm != null && !projectGroup.registrationForm.isEmpty()) ||
				(projectGroup.signupEnquiryForm != null && !projectGroup.signupEnquiryForm.isEmpty())
		);
		List<InvitationModel> invitations = invitationsService.getInvitations(projectGroup).stream()
				.filter(model -> model.anyFieldContains(searchField.getValue()))
				.collect(Collectors.toList());
		grid.setItems(invitations);
	}
}
