/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import static com.vaadin.flow.component.icon.VaadinIcon.COG_O;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;
import static io.imunity.vaadin.elements.CSSVars.BIG_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM_ITEM;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.facilities", parent = "WebConsoleMenu.authentication")
@Route(value = "/facilities", layout = ConsoleMenu.class)
public class FacilitiesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AuthenticatorsController controller;
	private final AuthenticationFlowsController flowsController;
	private GridWithActionColumn<AuthenticationFlowEntry> flowsGrid;
	private GridWithActionColumn<AuthenticatorEntry> authenticatorsGrid;


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
		authenticatorsGrid = new GridWithActionColumn<>(msg::getMessage, getAuthenticatorsActionsHandlers());
		authenticatorsGrid.addShowDetailsColumn(new ComponentRenderer<>(this::getDetailsComponent));
		authenticatorsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<AuthenticatorEntry> authenticatorNameColumn = authenticatorsGrid
				.addComponentColumn(entry -> new RouterLink(entry.authenticator().id, AuthenticatorEditView.class,
						entry.authenticator().id))
				.setHeader(msg.getMessage("AuthenticationFlowsComponent.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.authenticator().id));
		authenticatorsGrid.setItems(controller.getAllAuthenticators());
		authenticatorsGrid.sort(GridSortOrder.asc(authenticatorNameColumn)
				.build());

		flowsGrid = new GridWithActionColumn<>(msg::getMessage, getFlowsActionsHandlers());
		flowsGrid.addShowDetailsColumn(new ComponentRenderer<>(this::getDetailsComponent));
		flowsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<AuthenticationFlowEntry> nameColumn = flowsGrid
				.addComponentColumn(entry -> new RouterLink(entry.flow.getName(), AuthenticationFlowEditView.class,
						entry.flow.getName()))
				.setHeader(msg.getMessage("AuthenticationFlowsComponent.nameCaption"))
				.setAutoWidth(
						true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.flow.getName()));
		flowsGrid.setItems(flowsController.getFlows());
		flowsGrid.sort(GridSortOrder.asc(nameColumn).build());

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
	
	private List<SingleActionHandler<AuthenticatorEntry>> getAuthenticatorsActionsHandlers()
	{
		SingleActionHandler<AuthenticatorEntry> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, AuthenticatorEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<AuthenticatorEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, AuthenticatorEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);

	}

	private void gotoEdit(AuthenticatorEntry next)
	{
		UI.getCurrent()
				.navigate(AuthenticatorEditView.class, next.authenticator().id);
	}

	
	private List<SingleActionHandler<AuthenticationFlowEntry>> getFlowsActionsHandlers()
	{
		SingleActionHandler<AuthenticationFlowEntry> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, AuthenticationFlowEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<AuthenticationFlowEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, AuthenticationFlowEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);

	}

	private void gotoEdit(AuthenticationFlowEntry e)
	{
		UI.getCurrent()
				.navigate(AuthenticatorEditView.class, e.flow.getName());
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
}
