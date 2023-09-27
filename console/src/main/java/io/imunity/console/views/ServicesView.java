/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.vaadin.elements.ActionMenu;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.MenuButton;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceTypeInfoHelper;

import jakarta.annotation.security.PermitAll;
import java.util.Collections;

import static com.vaadin.flow.component.icon.VaadinIcon.*;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.services")
@Route(value = "/services", layout = ConsoleMenu.class)
public class ServicesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final ServicesController servicesController;
	private final Grid<ServiceDefinition> servicesGrid;
	ServicesView(MessageSource msg, ServicesController servicesController)
	{
		this.msg = msg;
		this.servicesController = servicesController;
		VerticalLayout layout = new VerticalLayout();
		VerticalLayout buttonLayout = new VerticalLayout();
		buttonLayout.setAlignItems(FlexComponent.Alignment.END);
		buttonLayout.setPadding(false);
		Button addButton = new Button(msg.getMessage("addNew"), e -> UI.getCurrent().navigate(ServicesEditView.class));
		buttonLayout.add(addButton);
		buttonLayout.setWidthFull();

		servicesGrid = new Grid<>();
		servicesGrid.addComponentColumn(e -> new RouterLink(e.getName(), ServicesEditView.class))
				.setHeader(msg.getMessage("ServicesView.nameCaption"))
				.setSortable(true)
				.setAutoWidth(true)
				.setComparator((e1, e2) -> e2.getName().compareTo(e1.getName()))
				.setId("name");

		servicesGrid.addComponentColumn(e -> getStatusIcon(e.getState()))
				.setHeader(msg.getMessage("ServicesView.statusCaption"))
				.setAutoWidth(true);

		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getType(msg, e.getType()))
				.setHeader(msg.getMessage("ServicesView.typeCaption"))
				.setAutoWidth(true);

		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getBinding(msg, e.getBinding()))
				.setHeader(msg.getMessage("ServicesView.bindingCaption"))
				.setAutoWidth(true)
				.setFlexGrow(30);

		servicesGrid.addComponentColumn(this::getHamburgerActionsHandlers)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.CENTER)
				.setAutoWidth(true);

		servicesGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		servicesGrid.setItems(servicesController.getServices());

		layout.add(buttonLayout);
		layout.add(servicesGrid);
		getContent().add(layout);
	}

	private Component getHamburgerActionsHandlers(ServiceDefinition serviceDefinition)
	{
		ActionMenu actionMenu = new ActionMenu();

		MenuButton removeButton = new MenuButton(msg.getMessage("remove"), TRASH);
		actionMenu.addItem(removeButton, e ->
		{
			tryRemove(serviceDefinition);
			refresh();
		});

		MenuButton deployButton = new MenuButton(msg.getMessage("ServicesView.deploy"), PLAY);
		actionMenu.addItem(deployButton, e ->
		{
			servicesController.deploy(serviceDefinition);
			refresh();
		});
		deployButton.setEnabled(serviceDefinition.getState().equals(Endpoint.EndpointState.UNDEPLOYED));

		MenuButton undeployButton = new MenuButton(msg.getMessage("ServicesView.undeploy"), BAN);
		actionMenu.addItem(undeployButton, e ->
		{
			servicesController.undeploy(serviceDefinition);
			refresh();
		});
		undeployButton.setEnabled(serviceDefinition.getState().equals(Endpoint.EndpointState.DEPLOYED));

		MenuButton reloadButton = new MenuButton(msg.getMessage("ServicesView.reload"), RETWEET);
		actionMenu.addItem(reloadButton, e ->
		{
			servicesController.reload(serviceDefinition);
			refresh();
		});

		Icon generalSettings = COGS.create();
		generalSettings.setTooltipText(msg.getMessage("ServicesView.generalConfig"));
		generalSettings.addClickListener(e -> UI.getCurrent().navigate(ServicesEditView.class));

		Icon signIn = SIGN_IN.create();
		signIn.setTooltipText(msg.getMessage("ServicesView.authenticationConfig"));
		signIn.addClickListener(e -> UI.getCurrent().navigate(ServicesEditView.class));

		return new HorizontalLayout(generalSettings, signIn, actionMenu.getTarget());
	}

	private Component getStatusIcon(Endpoint.EndpointState state)
	{
		return state == Endpoint.EndpointState.DEPLOYED ? VaadinIcon.CHECK_CIRCLE_O.create() : VaadinIcon.BAN.create();
	}

	void refresh()
	{
		servicesGrid.setItems(servicesController.getServices());
	}

	private void tryRemove(ServiceDefinition endpoint)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Collections.singletonList(endpoint.getName()));

		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("ServicesView.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> servicesController.remove(endpoint),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

}
