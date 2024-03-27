/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.policy_documents;

import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.GridSearchFieldFactory;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.settings.policyDocuments", parent = "WebConsoleMenu.settings")
@Route(value = "/policy-documents", layout = ConsoleMenu.class)
public class PolicyDocumentsView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final PolicyDocumentsController controller;
	private GridWithActionColumn<PolicyDocumentEntry> policyDocsGrid;
	
	PolicyDocumentsView(MessageSource msg, PolicyDocumentsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		init();
	}

	public void init()
	{
		policyDocsGrid = new GridWithActionColumn<PolicyDocumentEntry>(
				msg::getMessage, getActionsHandlers());
		policyDocsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<PolicyDocumentEntry> nameColumn = policyDocsGrid
				.addComponentColumn(e -> new RouterLink(e.name, PolicyDocumentEditView.class, String.valueOf(e.id)))
				.setHeader(msg.getMessage("PolicyDocumentsView.name"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(e -> e.name));
		policyDocsGrid.addColumn(e -> String.valueOf(e.revision))
				.setHeader(msg.getMessage("PolicyDocumentsView.version"))
				.setAutoWidth(true)
				.setSortable(true);
		policyDocsGrid.addColumn(e -> msg.getMessage("PolicyDocumentType." + e.contentType.toString()))
				.setHeader(msg.getMessage("PolicyDocumentsView.type"))
				.setAutoWidth(true)
				.setSortable(true);
		policyDocsGrid.sort(GridSortOrder.desc(nameColumn)
				.build());

		SearchField search = GridSearchFieldFactory.generateSearchField(policyDocsGrid, msg::getMessage);
		Toolbar<PolicyDocumentEntry> toolbar = new Toolbar<>();
		toolbar.addSearch(search);
		toolbar.setJustifyContentMode(JustifyContentMode.END);
		toolbar.setSizeFull();
		ComponentWithToolbar gridWithToolbar = new ComponentWithToolbar(policyDocsGrid, toolbar);
		gridWithToolbar.setSpacing(false);
		gridWithToolbar.setSizeFull();
		policyDocsGrid.setItems(new ArrayList<>(controller.getPolicyDocuments()));
		getContent()
				.add(new VerticalLayout(createHeaderActionLayout(msg, PolicyDocumentEditView.class), gridWithToolbar));
	}

	private List<SingleActionHandler<PolicyDocumentEntry>> getActionsHandlers()
	{
		SingleActionHandler<PolicyDocumentEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, PolicyDocumentEntry.class)
				.withHandler(this::tryRemove)
				.build();

		SingleActionHandler<PolicyDocumentEntry> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, PolicyDocumentEntry.class)
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.build();

		return Arrays.asList(edit, remove);
	}

	private void gotoEdit(PolicyDocumentEntry policy)
	{
		UI.getCurrent()
				.navigate(PolicyDocumentEditView.class, policy.getId()
						.toString());
	}

	private void tryRemove(Set<PolicyDocumentEntry> entry)
	{
		PolicyDocumentEntry item = entry.iterator()
				.next();
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(item.name));
		new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("PolicyDocumentsView.confirmDelete", confirmText), msg.getMessage("ok"),
				e -> remove(item), msg.getMessage("cancel"), e ->
				{
				}).open();
	}

	private void remove(PolicyDocumentEntry e)
	{
		controller.removePolicyDocument(e.id);
		policyDocsGrid.removeElement(e);
	}
}
