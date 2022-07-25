/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.user_updates;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.imunity.upman.av23.front.components.*;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.av23.front.views.UpManMenu;
import pl.edu.icm.unity.MessageSource;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.BAN;
import static com.vaadin.flow.component.icon.VaadinIcon.CHECK_CIRCLE_O;

@Route(value = "/user-updates", layout = UpManMenu.class)
public class UserUpdatesView extends UnityViewComponent
{
	private final MessageSource msg;
	private final UpdateRequestsService updateRequestsService;

	private final Grid<UpdateRequestModel> grid;
	private final TextField searchField;
	private final VerticalLayout linksLayout;

	private ProjectGroup projectGroup;

	public UserUpdatesView(MessageSource msg, UpdateRequestsService updateRequestsService)
	{
		this.msg = msg;
		this.updateRequestsService = updateRequestsService;

		getContent().add();
		grid = createGrid();
		searchField = createSearchField();
		linksLayout = createLinksLayout();

		getContent().add(
				linksLayout,
				createMenuAndSearchLayout(createContextMenu(grid::getSelectedItems), searchField),
				grid
		);

		loadData();
	}

	private VerticalLayout createLinksLayout()
	{
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.getStyle().set("gap", "0");
		return verticalLayout;
	}

	private Grid<UpdateRequestModel> createGrid()
	{
		return new UpdateRequestGrid(msg, this::createGridContextMenu, getContent());
	}

	private TextField createSearchField()
	{
		return new SearchField(msg.getMessage("Search"), this::loadData);
	}

	private HorizontalLayout createMenuAndSearchLayout(Component memberActionMenu, TextField textField)
	{
		HorizontalLayout layout = new HorizontalLayout(memberActionMenu, textField);
		layout.setAlignItems(FlexComponent.Alignment.END);
		layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		layout.getStyle().set("padding-left", "1.2em");
		return layout;
	}

	private Component createGridContextMenu(UpdateRequestModel model)
	{
		return createContextMenu(() -> Set.of(model));
	}

	private Component createContextMenu(Supplier<Set<UpdateRequestModel>> updateRequestGetter)
	{
		ActionMenu menu = new ActionMenu();

		menu.addItem(
				new MenuButton(msg.getMessage("UpdateRequestsComponent.accepted"), CHECK_CIRCLE_O),
				event ->
				{
					updateRequestsService.accept(projectGroup, updateRequestGetter.get());
					loadData();
				}
		);
		menu.addItem(
				new MenuButton(msg.getMessage("UpdateRequestsComponent.declineRequestAction"), BAN),
				event ->
				{
					updateRequestsService.decline(projectGroup, updateRequestGetter.get());
					loadData();
				}
		);

		return menu.getTarget();
	}

	@Override
	public void loadData()
	{
		projectGroup = ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class);

		linksLayout.removeAll();
		updateRequestsService.getProjectRegistrationFormLink(projectGroup)
			.ifPresent(link -> linksLayout.add(new BlankPageAnchor(link, msg.getMessage("UpdateRequestsComponent.selfSignUpForm"))));
		updateRequestsService.getProjectSingUpEnquiryFormLink(projectGroup)
				.ifPresent(link -> linksLayout.add(new BlankPageAnchor(link, msg.getMessage("UpdateRequestsComponent.signUpForm"))));
		updateRequestsService.getProjectUpdateMembershipEnquiryFormLink(projectGroup)
				.ifPresent(link -> linksLayout.add(new BlankPageAnchor(link, msg.getMessage("UpdateRequestsComponent.updateForm"))));

		grid.setItems(updateRequestsService.getUpdateRequests(projectGroup).stream()
				.filter(request -> request.anyFieldContains(searchField.getValue()))
				.collect(Collectors.toList())
		);
	}
}
