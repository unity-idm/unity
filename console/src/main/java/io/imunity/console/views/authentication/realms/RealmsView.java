/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.realms;

import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM_ITEM;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
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
@Breadcrumb(key = "WebConsoleMenu.authentication.realms", parent = "WebConsoleMenu.authentication")
@Route(value = "/realms", layout = ConsoleMenu.class)
public class RealmsView extends ConsoleViewComponent
{
	private final RealmsController realmsController;
	private final MessageSource msg;
	private GridWithActionColumn<AuthenticationRealmEntry> realmsGrid;

	RealmsView(MessageSource msg, RealmsController realmsController)
	{
		this.realmsController = realmsController;
		this.msg = msg;
		init();
	}

	public void init()
	{
		realmsGrid = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
		realmsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		realmsGrid.addShowDetailsColumn(new ComponentRenderer<>(this::getDetailsComponent));
		Grid.Column<AuthenticationRealmEntry> nameColumn = realmsGrid.addComponentColumn(this::createNameWithDetailsArrow)
				.setHeader(msg.getMessage("AuthenticationRealmsView.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.realm.getName()));
		realmsGrid.sort(GridSortOrder.asc(nameColumn).build());
		realmsGrid.setItems(realmsController.getRealms());

		VerticalLayout layout = new VerticalLayout(createHeaderActionLayout(msg, RealmEditView.class), realmsGrid);
		layout.setSpacing(false);
		getContent().add(layout);
	}

	private List<SingleActionHandler<AuthenticationRealmEntry>> getActionsHandlers()
	{
		SingleActionHandler<AuthenticationRealmEntry> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, AuthenticationRealmEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<AuthenticationRealmEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, AuthenticationRealmEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);

	}

	private void gotoEdit(AuthenticationRealmEntry next)
	{
		UI.getCurrent()
				.navigate(RealmEditView.class, next.realm.getName());
	}



	private FormLayout getDetailsComponent(AuthenticationRealmEntry realm)
	{
		FormLayout wrapper = new FormLayout();
		FormLayout.FormItem formItem = wrapper.addFormItem(
				new Span(String.join(", ", realm.endpoints)),
				msg.getMessage("AuthenticationRealmsView.endpointsCaption")
		);
		formItem.addClassName(GRID_DETAILS_FORM_ITEM.getName());
		wrapper.addClassName(GRID_DETAILS_FORM.getName());
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
	private RouterLink createNameWithDetailsArrow(AuthenticationRealmEntry entry)
	{
		return new RouterLink(entry.realm.getName(), RealmEditView.class, entry.realm.getName());
		
	}
}
