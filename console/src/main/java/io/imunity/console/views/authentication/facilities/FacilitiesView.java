/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
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

import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM_ITEM;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.facilities", parent = "WebConsoleMenu.authentication")
@Route(value = "/facilities", layout = ConsoleMenu.class)
public class FacilitiesView extends ConsoleViewComponent
{
	private final AuthenticationFlowsController flowsController;
	private final MessageSource msg;
	private Grid<AuthenticationFlowEntry> flowsGrid;

	FacilitiesView(MessageSource msg, AuthenticationFlowsController flowsController)
	{
		this.msg = msg;
		this.flowsController = flowsController;
		initUI();
	}

	private void initUI()
	{
		flowsGrid = new Grid<>();
		flowsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		flowsGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::getDetailsComponent));
		Grid.Column<AuthenticationFlowEntry> nameColumn = flowsGrid.addComponentColumn(this::createNameWithDetailsArrow)
				.setHeader(msg.getMessage("AuthenticationFlowsComponent.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.flow.getName()));
		flowsGrid.setItems(flowsController.getFlows());
		flowsGrid.sort(GridSortOrder.asc(nameColumn).build());
		flowsGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		H3 certCaption = new H3(msg.getMessage("AuthenticationFlowsComponent.caption"));
		VerticalLayout main = new VerticalLayout(certCaption, createHeaderActionLayout(msg, AuthenticationFlowEditView.class), flowsGrid);
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

	private void tryRemove(AuthenticationFlowEntry flow)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(flow.flow.getName()));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AuthenticationFlowsComponent.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(flow),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private HorizontalLayout createNameWithDetailsArrow(AuthenticationFlowEntry entry)
	{
		RouterLink label = new RouterLink(entry.flow.getName(), AuthenticationFlowEditView.class, entry.flow.getName());
		Icon openIcon = VaadinIcon.ANGLE_RIGHT.create();
		Icon closeIcon = VaadinIcon.ANGLE_DOWN.create();
		openIcon.setVisible(!flowsGrid.isDetailsVisible(entry));
		closeIcon.setVisible(flowsGrid.isDetailsVisible(entry));
		openIcon.addClickListener(e -> flowsGrid.setDetailsVisible(entry, true));
		closeIcon.addClickListener(e -> flowsGrid.setDetailsVisible(entry, false));
		return new HorizontalLayout(openIcon, closeIcon, label);
	}

}