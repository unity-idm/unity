/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credentials;

import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.localCredentials", parent = "WebConsoleMenu.authentication")
@Route(value = "/credentials", layout = ConsoleMenu.class)
public class CredentialsView extends ConsoleViewComponent
{
	private final CredentialsController controller;
	private final MessageSource msg;
	private final EventsBus bus;
	private GridWithActionColumn<CredentialDefinition> credList;

	CredentialsView(MessageSource msg, CredentialsController controller)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent()
				.getEventBus();
		init();
	}

	private void init()
	{
		VerticalLayout buttonsBar = createHeaderActionLayout(msg, CredentialsEditView.class);

		credList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
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
		credList.setItems(controller.getCredentials());
		credList.sort(GridSortOrder.asc(nameColumn)
				.build());

		VerticalLayout main = new VerticalLayout();
		main.add(buttonsBar, credList);
		getContent().add(main);
	}

	private List<SingleActionHandler<CredentialDefinition>> getActionsHandlers()
	{
		SingleActionHandler<CredentialDefinition> show = SingleActionHandler
				.builder4ShowDetails(msg::getMessage, CredentialDefinition.class)
				.withHandler(r -> gotoShowDetails(r.iterator()
						.next()))
				.withDisabledPredicate(r -> !r.isReadOnly())
				.hideIfInactive()
				.build();

		SingleActionHandler<CredentialDefinition> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, CredentialDefinition.class)
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.withDisabledPredicate(r -> r.isReadOnly())
				.hideIfInactive()
				.build();

		SingleActionHandler<CredentialDefinition> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, CredentialDefinition.class)
				.withHandler(r -> tryRemove(r.iterator()
						.next()))
				.build();

		return Arrays.asList(show, edit, remove);
	}

	private void gotoShowDetails(CredentialDefinition cred)
	{
		UI.getCurrent()
				.navigate(CredentialsInfoView.class, cred.getName());
	}

	private void gotoEdit(CredentialDefinition cred)
	{
		UI.getCurrent()
				.navigate(CredentialsEditView.class, cred.getName());
	}

	private void tryRemove(CredentialDefinition cred)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(cred.getName()));
		new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("LocalCredentialsView.confirmDelete", confirmText), msg.getMessage("ok"),
				e -> remove(cred), msg.getMessage("cancel"), e ->
				{
				}).open();
	}

	private void remove(CredentialDefinition cred)
	{
		controller.removeCredential(cred, bus);
		credList.setItems(controller.getCredentials());
	}

}
