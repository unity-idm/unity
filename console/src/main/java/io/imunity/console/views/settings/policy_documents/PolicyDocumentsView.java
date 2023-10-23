/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.policy_documents;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
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

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.settings.policyDocuments", parent = "WebConsoleMenu.settings")
@Route(value = "/policy-documents", layout = ConsoleMenu.class)
public class PolicyDocumentsView extends ConsoleViewComponent
{

	private final MessageSource msg;
	private final PolicyDocumentsController controller;
	private final TextField search = new TextField();
	private Grid<PolicyDocumentEntry> policyDocsGrid;

	PolicyDocumentsView(MessageSource msg, PolicyDocumentsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		init();
	}

	public void init()
	{
		policyDocsGrid = new Grid<>();
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

		policyDocsGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		policyDocsGrid.sort(GridSortOrder.desc(nameColumn).build());
		loadData();
		getContent().add(new VerticalLayout(createHeaderLayout(), policyDocsGrid));
	}

	private VerticalLayout createHeaderLayout()
	{
		VerticalLayout headerLayout = createHeaderActionLayout(msg, PolicyDocumentEditView.class);
		search.setValueChangeMode(ValueChangeMode.EAGER);
		search.setPlaceholder(msg.getMessage("search"));
		headerLayout.add(search);
		return headerLayout;
	}

	private void loadData()
	{
		GridListDataView<PolicyDocumentEntry> messageTemplateEntryGridListDataView = policyDocsGrid.setItems(controller.getPolicyDocuments());
		messageTemplateEntryGridListDataView.addFilter(entry -> entry.anyFieldContains(search.getValue()));
		search.addValueChangeListener(e -> messageTemplateEntryGridListDataView.refreshAll());
	}

	private Component createRowActionMenu(PolicyDocumentEntry entry)
	{
		Icon generalSettings = new ActionIconBuilder()
				.icon(EDIT)
				.tooltipText(msg.getMessage("edit"))
				.navigation(PolicyDocumentEditView.class, String.valueOf(entry.id))
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

	private void tryRemove(PolicyDocumentEntry entry)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(entry.name));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("PolicyDocumentsView.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(entry),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private void remove(PolicyDocumentEntry e)
	{
		controller.removePolicyDocument(e.id);
		loadData();
	}
}
