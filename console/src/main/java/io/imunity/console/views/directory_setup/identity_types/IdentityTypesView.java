/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.identity_types;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.grid.GridWithActionColumn;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.identityTypes", parent = "WebConsoleMenu.directorySetup")
@Route(value = "/identity-types", layout = ConsoleMenu.class)
public class IdentityTypesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final IdentityTypesController controller;
	private final NotificationPresenter notificationPresenter;

	private GridWithActionColumn<IdentityTypeEntry> identityTypesGrid;

	IdentityTypesView(MessageSource msg, IdentityTypesController controller,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		init();
	}

	private void init()
	{
		identityTypesGrid = new GridWithActionColumn<IdentityTypeEntry>(msg, getActionsHandlers());
		identityTypesGrid.addShowDetailsColumn(new ComponentRenderer<>(this::getDetailsComponent));
		identityTypesGrid.setMultiSelect(false);
		
		Grid.Column<IdentityTypeEntry> nameColumn = identityTypesGrid
				.addComponentColumn(this::createNameColumn)
				.setHeader(msg.getMessage("AuthenticationFlowsComponent.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.type()
						.getName()));
		identityTypesGrid.addBooleanColumn(v -> v.typeDefinition()
				.isDynamic())
				.setHeader(msg.getMessage("IdentityTypesView.automaticCaption"));
		
		identityTypesGrid.addBooleanColumn(v ->v.type()
				.isSelfModificable())
				.setHeader(msg.getMessage("IdentityTypesView.modifiableByUserCaption"));
		
		try
		{
			identityTypesGrid.setItems(controller.getIdentityTypes());
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());

		}

		identityTypesGrid.sort(GridSortOrder.asc(nameColumn)
				.build());
		getContent().add(identityTypesGrid);

	}

	private List<SingleActionHandler<IdentityTypeEntry>> getActionsHandlers()
	{
		SingleActionHandler<IdentityTypeEntry> edit = SingleActionHandler.builder4Edit(msg, IdentityTypeEntry.class)
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.build();

		return Arrays.asList(edit);
	}

	private void gotoEdit(IdentityTypeEntry next)
	{
		UI.getCurrent()
				.navigate(EditIdentityTypeView.class, next.type()
						.getName());
	}

	private RouterLink createNameColumn(IdentityTypeEntry entry)
	{
		return new RouterLink(entry.type()
				.getName(), EditIdentityTypeView.class,
				entry.type()
						.getName());
	}

	private FormLayout getDetailsComponent(IdentityTypeEntry i)
	{
		FormLayout wrapper = new FormLayout();
		NativeLabel label = new NativeLabel(i.type()
				.getDescription());
		label.setWidthFull();
		wrapper.addFormItem(label, msg.getMessage("IdentityTypesView.descriptionLabelCaption"));
		return wrapper;
	}

}
