/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionIconBuilder;
import io.imunity.vaadin.elements.Breadcrumb;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.Comparator;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;
import static io.imunity.vaadin.elements.CSSVars.BIG_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM_ITEM;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.facilities", parent = "WebConsoleMenu.authentication")
@Route(value = "/facilities", layout = ConsoleMenu.class)
public class FacilitiesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AuthenticatorsController controller;
	private final AuthenticationFlowsController flowsController;
	private Grid<AuthenticationFlowEntry> flowsGrid;
	private Grid<AuthenticatorEntry> authenticatorsGrid;


	FacilitiesView(MessageSource msg, AuthenticatorsController controller,
			AuthenticationFlowsController flowsController)
	{
		this.msg = msg;
		this.controller = controller;
		this.flowsController = flowsController;
		initUI();
	}

	private void initUI()
	{
		authenticatorsGrid = new Grid<>();
		authenticatorsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		authenticatorsGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::getDetailsComponent));
		Grid.Column<AuthenticatorEntry> authenticatorNameColumn = authenticatorsGrid.addComponentColumn(entry ->
				{
					RouterLink label = new RouterLink(entry.authenticator().id, AuthenticatorEditView.class,
							entry.authenticator().id);
					return createNameWithDetailsArrow(authenticatorsGrid, entry, label);
				})
				.setHeader(msg.getMessage("AuthenticationFlowsComponent.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.authenticator().id));
		authenticatorsGrid.setItems(controller.getAllAuthenticators());
		authenticatorsGrid.sort(GridSortOrder.asc(authenticatorNameColumn).build());
		authenticatorsGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		flowsGrid = new Grid<>();
		flowsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		flowsGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::getDetailsComponent));
		Grid.Column<AuthenticationFlowEntry> nameColumn = flowsGrid.addComponentColumn(entry ->
				{
					RouterLink label = new RouterLink(entry.flow.getName(), AuthenticationFlowEditView.class,
							entry.flow.getName());
					return createNameWithDetailsArrow(flowsGrid, entry, label);
				})
				.setHeader(msg.getMessage("AuthenticationFlowsComponent.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.flow.getName()));
		flowsGrid.setItems(flowsController.getFlows());
		flowsGrid.sort(GridSortOrder.asc(nameColumn).build());
		flowsGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		H3 authenticatorHeader = new H3(msg.getMessage("AuthenticatorsComponent.caption"));
		H3 authenticationFlowsHeader = new H3(msg.getMessage("AuthenticationFlowsComponent.caption"));
		authenticationFlowsHeader.getStyle().set("margin-top", BIG_MARGIN.value());
		VerticalLayout main = new VerticalLayout(
				authenticatorHeader,
				createAuthenticatorActionLayout(),
				authenticatorsGrid, new Hr(),
				authenticationFlowsHeader,
				createHeaderActionLayout(msg, AuthenticationFlowEditView.class),
				flowsGrid);
		main.setSpacing(false);
		getContent().add(main);
	}

	private FormLayout getDetailsComponent(AuthenticationFlowEntry flow)
	{
		FormLayout wrapper = new FormLayout();
		FormLayout.FormItem formItem = wrapper.addFormItem(
				new Span(String.join(", ", flow.endpoints)),
				msg.getMessage("AuthenticationFlowsComponent.endpointsCaption")
		);
		formItem.addClassName(GRID_DETAILS_FORM_ITEM.getName());
		wrapper.addClassName(GRID_DETAILS_FORM.getName());
		return wrapper;
	}

	private FormLayout getDetailsComponent(AuthenticatorEntry entry)
	{
		FormLayout wrapper = new FormLayout();
		FormLayout.FormItem formItem = wrapper.addFormItem(
				new Span(String.join(", ", entry.endpoints())),
				msg.getMessage("AuthenticationFlowsComponent.endpointsCaption")
		);
		formItem.addClassName(GRID_DETAILS_FORM_ITEM.getName());
		wrapper.addClassName(GRID_DETAILS_FORM.getName());
		return wrapper;
	}

	public VerticalLayout createAuthenticatorActionLayout()
	{
		VerticalLayout headerLayout = new VerticalLayout();
		headerLayout.setPadding(false);
		headerLayout.setSpacing(false);
		Button addButton = new Button(msg.getMessage("addNew"), e -> UI.getCurrent().navigate(AuthenticatorEditView.class));
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		Button wizard = new Button(msg.getMessage("AuthenticatorsComponent.test"), COG_O.create(),
				e -> controller.getWizard().open());
		headerLayout.setAlignItems(FlexComponent.Alignment.END);
		headerLayout.add(new HorizontalLayout(wizard, addButton));
		return headerLayout;
	}

	private Component createRowActionMenu(AuthenticatorEntry entry)
	{
		Icon generalSettings = new ActionIconBuilder()
				.icon(EDIT)
				.tooltipText(msg.getMessage("edit"))
				.navigation(AuthenticatorEditView.class, entry.authenticator().id)
				.build();

		Icon remove = new ActionIconBuilder()
				.icon(TRASH)
				.tooltipText(msg.getMessage("remove"))
				.clickListener(() -> tryRemove(entry))
				.build();

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings, remove);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private Component createRowActionMenu(AuthenticationFlowEntry entry)
	{
		Icon generalSettings = new ActionIconBuilder()
				.icon(EDIT)
				.tooltipText(msg.getMessage("edit"))
				.navigation(AuthenticationFlowEditView.class, entry.flow.getName())
				.build();

		Icon remove = new ActionIconBuilder()
				.icon(TRASH)
				.tooltipText(msg.getMessage("remove"))
				.clickListener(() -> tryRemove(entry))
				.build();

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings, remove);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private void remove(AuthenticationFlowEntry flow)
	{
		flowsController.removeFlow(flow.flow);
		flowsGrid.setItems(flowsController.getFlows());
	}

	private void remove(AuthenticatorEntry entry)
	{
		controller.removeAuthenticator(entry.authenticator());
		authenticatorsGrid.setItems(controller.getAllAuthenticators());
	}

	private void tryRemove(AuthenticatorEntry entry)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(entry.authenticator().id));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AuthenticatorsComponent.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(entry),
				msg.getMessage("cancel"),
				e ->
				{
				}
		).open();
	}

	private void tryRemove(AuthenticationFlowEntry flow)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(flow.flow.getName()));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AuthenticationFlowsComponent.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(flow),
				msg.getMessage("cancel"),
				e ->
				{
				}
		).open();
	}

	private static <T> HorizontalLayout createNameWithDetailsArrow(Grid<T> grid, T entry, RouterLink label)
	{
		Icon openIcon = VaadinIcon.ANGLE_RIGHT.create();
		Icon closeIcon = VaadinIcon.ANGLE_DOWN.create();
		openIcon.setVisible(!grid.isDetailsVisible(entry));
		closeIcon.setVisible(grid.isDetailsVisible(entry));
		openIcon.addClickListener(e -> grid.setDetailsVisible(entry, true));
		closeIcon.addClickListener(e -> grid.setDetailsVisible(entry, false));
		return new HorizontalLayout(openIcon, closeIcon, label);
	}

}
