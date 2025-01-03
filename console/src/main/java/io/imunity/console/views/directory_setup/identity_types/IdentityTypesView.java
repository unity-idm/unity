/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.identity_types;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.identityTypes", parent = "WebConsoleMenu.directorySetup")
@Route(value = "/identity-types", layout = ConsoleMenu.class)
public class IdentityTypesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final IdentityTypesController controller;
	private final NotificationPresenter notificationPresenter;

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
		GridWithActionColumn<IdentityTypeEntry> identityTypesGrid = new GridWithActionColumn<IdentityTypeEntry>(
				msg::getMessage, getActionsHandlers());
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
		getContent().setHeightFull();

	}

	private List<SingleActionHandler<IdentityTypeEntry>> getActionsHandlers()
	{
		SingleActionHandler<IdentityTypeEntry> edit = SingleActionHandler.builder4Edit(msg::getMessage, IdentityTypeEntry.class)
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
		wrapper.setWidthFull();
		NativeLabel label = new NativeLabel(i.type()
				.getDescription());
		label.setWidthFull();
		FormItem addFormItem = wrapper.addFormItem(label, msg.getMessage("IdentityTypesView.descriptionLabelCaption"));
		addFormItem.addClassName(CssClassNames.WIDTH_FULL.getName());
		return wrapper;
	}

}
