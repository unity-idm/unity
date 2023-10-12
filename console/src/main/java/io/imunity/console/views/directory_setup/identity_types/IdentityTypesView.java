/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.identity_types;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionIconBuilder;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Comparator;

import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.identityTypes")
@Route(value = "/identity-types", layout = ConsoleMenu.class)
public class IdentityTypesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final IdentityTypesController controller;
	private final NotificationPresenter notificationPresenter;

	private Grid<IdentityTypeEntry> identityTypesGrid;

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
		identityTypesGrid = new Grid<>();
		identityTypesGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		identityTypesGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::getDetailsComponent));

		Grid.Column<IdentityTypeEntry> nameColumn = identityTypesGrid
				.addComponentColumn(this::createNameWithDetailsArrow)
				.setHeader(msg.getMessage("AuthenticationFlowsComponent.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.type().getName()));
		identityTypesGrid.addComponentColumn(v -> generateCheckBox(v.typeDefinition().isDynamic()))
				.setHeader(msg.getMessage("IdentityTypesView.automaticCaption"));
		identityTypesGrid.addComponentColumn(v -> generateCheckBox(v.type().isSelfModificable()))
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
		identityTypesGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);
		getContent().add(identityTypesGrid);

	}

	private Checkbox generateCheckBox(boolean initialValue)
	{
		Checkbox checkbox = new Checkbox(initialValue);
		checkbox.setReadOnly(true);
		return checkbox;
	}

	private Component createRowActionMenu(IdentityTypeEntry entry)
	{
		Icon generalSettings = new ActionIconBuilder()
				.setIcon(EDIT)
				.setTooltipText(msg.getMessage("edit"))
				.setNavigation(EditIdentityTypeView.class, entry.type().getName())
				.build();

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private HorizontalLayout createNameWithDetailsArrow(IdentityTypeEntry entry)
	{
		RouterLink label = new RouterLink(entry.type().getName(), EditIdentityTypeView.class, entry.type().getName());
		Icon openIcon = VaadinIcon.ANGLE_RIGHT.create();
		Icon closeIcon = VaadinIcon.ANGLE_DOWN.create();
		openIcon.setVisible(!identityTypesGrid.isDetailsVisible(entry));
		closeIcon.setVisible(identityTypesGrid.isDetailsVisible(entry));
		openIcon.addClickListener(e -> identityTypesGrid.setDetailsVisible(entry, true));
		closeIcon.addClickListener(e -> identityTypesGrid.setDetailsVisible(entry, false));
		return new HorizontalLayout(openIcon, closeIcon, label);
	}

	private FormLayout getDetailsComponent(IdentityTypeEntry i)
	{
		FormLayout wrapper = new FormLayout();
		NativeLabel label = new NativeLabel(i.type().getDescription());
		label.setWidthFull();
		wrapper.addFormItem(label,
				msg.getMessage("IdentityTypesView.descriptionLabelCaption"));
		return wrapper;
	}

}
