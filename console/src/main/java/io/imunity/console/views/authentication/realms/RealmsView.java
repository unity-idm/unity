/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.realms;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
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
import io.imunity.vaadin.elements.Breadcrumb;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.Comparator;

import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.realms")
@Route(value = "/realms", layout = ConsoleMenu.class)
public class RealmsView extends ConsoleViewComponent
{
	private final RealmsController realmsController;
	private final MessageSource msg;
	private Grid<AuthenticationRealmEntry> realmsGrid;

	RealmsView(MessageSource msg, RealmsController realmsController)
	{
		this.realmsController = realmsController;
		this.msg = msg;
		init();
	}

	public void init()
	{
		realmsGrid = new Grid<>();
		realmsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<AuthenticationRealmEntry> nameColumn = realmsGrid.addComponentColumn(this::createNameWithDetailsArrow)
				.setHeader(msg.getMessage("AuthenticationRealmsView.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.realm.getName()));
		realmsGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);
		realmsGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::getDetailsComponent));

		realmsGrid.sort(GridSortOrder.asc(nameColumn).build());
		realmsGrid.setItems(realmsController.getRealms());

		VerticalLayout layout = new VerticalLayout(createHeaderActionLayout(msg, RealmEditView.class), realmsGrid);
		layout.setSpacing(false);
		getContent().add(layout);
	}

	private Component createRowActionMenu(AuthenticationRealmEntry entry)
	{
		Icon generalSettings = EDIT.create();
		generalSettings.setTooltipText(msg.getMessage("edit"));
		generalSettings.getStyle().set("cursor", "pointer");
		generalSettings.addClickListener(e -> UI.getCurrent().navigate(RealmEditView.class, String.valueOf(entry.realm.getName())));
		generalSettings.getElement().setAttribute("onclick", "event.stopPropagation();");

		Icon remove = TRASH.create();
		remove.setTooltipText(msg.getMessage("remove"));
		remove.getStyle().set("cursor", "pointer");
		remove.addClickListener(e -> tryRemove(entry));
		remove.getElement().setAttribute("onclick", "event.stopPropagation();");

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings, remove);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private FormLayout getDetailsComponent(AuthenticationRealmEntry realm)
	{
		FormLayout wrapper = new FormLayout();
		wrapper.addFormItem(
				new Span(String.join(", ", realm.endpoints)),
				msg.getMessage("AuthenticationRealmsView.endpointsCaption")
		);
		wrapper.getStyle().set("margin-bottom", "0.75em");
		return wrapper;
	}

	private void tryRemove(AuthenticationRealmEntry entry)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(entry.realm.getName()));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AuthenticationRealmsView.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(entry),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private void remove(AuthenticationRealmEntry entry)
	{
		realmsController.removeRealm(entry.realm);
		realmsGrid.setItems(realmsController.getRealms());
	}
	private HorizontalLayout createNameWithDetailsArrow(AuthenticationRealmEntry entry)
	{
		RouterLink label = new RouterLink(entry.realm.getName(), RealmEditView.class, entry.realm.getName());
		Icon openIcon = VaadinIcon.ANGLE_RIGHT.create();
		Icon closeIcon = VaadinIcon.ANGLE_DOWN.create();
		openIcon.setVisible(!realmsGrid.isDetailsVisible(entry));
		closeIcon.setVisible(realmsGrid.isDetailsVisible(entry));
		openIcon.addClickListener(e -> realmsGrid.setDetailsVisible(entry, true));
		closeIcon.addClickListener(e -> realmsGrid.setDetailsVisible(entry, false));
		return new HorizontalLayout(openIcon, closeIcon, label);
	}
}
