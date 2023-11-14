/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credentials;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionIconBuilder;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.Comparator;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;


@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.localCredentials", parent = "WebConsoleMenu.authentication")
@Route(value = "/credentials", layout = ConsoleMenu.class)
public class CredentialsView extends ConsoleViewComponent
{
	private final CredentialsController controller;
	private final MessageSource msg;
	private final EventsBus bus;
	private Grid<CredentialDefinition> credList;

	CredentialsView(MessageSource msg, CredentialsController controller)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();
		init();
	}

	private void init()
	{
		VerticalLayout buttonsBar = createHeaderActionLayout(msg, CredentialsEditView.class);

		credList = new Grid<>();
		credList.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<CredentialDefinition> nameColumn = credList.addComponentColumn(entry ->
				{
					if (!entry.isReadOnly())
						return new RouterLink(entry.getName(), CredentialsEditView.class, entry.getName());
					else
						return new RouterLink(entry.getName(), CredentialsInfoView.class, entry.getName());
				})
				.setHeader(msg.getMessage("LocalCredentialsView.nameCaption"))
				.setSortable(true)
				.setComparator(Comparator.comparing(CredentialDefinition::getName));

		credList.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		credList.setItems(controller.getCredentials());
		credList.sort(GridSortOrder.asc(nameColumn).build());

		VerticalLayout main = new VerticalLayout();
		main.add(buttonsBar, credList);
		getContent().add(main);
	}

	private Component createRowActionMenu(CredentialDefinition credentialDefinition)
	{
		Icon generalSettings = new ActionIconBuilder()
				.icon(EDIT)
				.tooltipText(msg.getMessage("edit"))
				.navigation(CredentialsEditView.class, credentialDefinition.getName())
				.build();
		Icon remove = new ActionIconBuilder()
				.icon(TRASH)
				.tooltipText(msg.getMessage("remove"))
				.clickListener(() -> tryRemove(credentialDefinition))
				.build();
		Icon info = new ActionIconBuilder()
				.icon(INFO)
				.tooltipText(msg.getMessage("showDetails"))
				.navigation(CredentialsInfoView.class, credentialDefinition.getName())
				.build();

		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		if(credentialDefinition.isReadOnly())
			horizontalLayout.add(info, remove);
		else
			horizontalLayout.add(generalSettings, remove);

		return horizontalLayout;
	}

	private void tryRemove(CredentialDefinition cred)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(cred.getName()));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("LocalCredentialsView.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(cred),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private void remove(CredentialDefinition cred)
	{
		controller.removeCredential(cred, bus);
		credList.setItems(controller.getCredentials());
	}

}
