/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.group_details;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;


class AttributeStatementsComponent extends VerticalLayout
{
	private final MessageSource msg;
	private final AttributeStatementController controller;
	private final EventsBus bus;
	private final GridWithActionColumn<AttrStatementWithId> attrStatementsGrid;

	private AttrStatementWithId draggedItem;
	private Group group;

	AttributeStatementsComponent(MessageSource msg, AttributeStatementController controller)
	{
		this.msg = msg;
		this.controller = controller;
		this.bus = WebSession.getCurrent().getEventBus();

		attrStatementsGrid = new GridWithActionColumn<>(msg::getMessage, Collections.emptyList());
		attrStatementsGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
		attrStatementsGrid.addShowDetailsColumn(new ComponentRenderer<>(this::getDetailsComponent));
		attrStatementsGrid.addColumn(attrStatementWithId -> msg.getMessage("AttributeStatements.nameValue", attrStatementWithId.toShortString()))
				.setHeader(msg.getMessage("AttributeStatements.nameCaption"))
				.setAutoWidth(true)
				.setResizable(true);
		attrStatementsGrid.addHamburgerActions(getRowActionsHandlers());
		attrStatementsGrid.setSizeFull();
		attrStatementsGrid.setMultiSelect(true);
		attrStatementsGrid.setRowsDraggable(true);
		attrStatementsGrid.addDragStartListener(event ->
		{
			draggedItem = event.getDraggedItems().iterator().next();
			attrStatementsGrid.setDropMode(GridDropMode.BETWEEN);
		});
		attrStatementsGrid.addDragEndListener(event ->
		{
			draggedItem = null;
			attrStatementsGrid.setDropMode(null);
		});
		attrStatementsGrid.addDropListener(event ->
		{
			AttrStatementWithId dropOverItem = event.getDropTargetItem().get();
			if (!dropOverItem.equals(draggedItem))
			{
				attrStatementsGrid.getElements().remove(draggedItem);
				int dropIndex =
						attrStatementsGrid.getElements().indexOf(dropOverItem) + (event.getDropLocation() == GridDropLocation.BELOW ? 1 : 0);
				attrStatementsGrid.getElements().add(dropIndex, draggedItem);
				attrStatementsGrid.getDataProvider().refreshAll();
				updateGroup();
			}
		});

		ActionMenuWithHandlerSupport<AttrStatementWithId> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(getGlobalHamburgerActionsHandlers());
		attrStatementsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		Toolbar<AttrStatementWithId> toolbar = new Toolbar<>();
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addCompactHamburger(hamburgerMenu, Alignment.END);
		ComponentWithToolbar attrStatementsGridWithToolbar = new ComponentWithToolbar(attrStatementsGrid, toolbar);
		attrStatementsGridWithToolbar.setSizeFull();
		attrStatementsGridWithToolbar.setClassName(SMALL_GAP.getName());

		VerticalLayout main = new VerticalLayout();
		main.add(attrStatementsGridWithToolbar);
		main.setSizeFull();
		main.setPadding(false);
		setSizeFull();
		setPadding(false);
		add(main);
	}

	private FormLayout getDetailsComponent(AttrStatementWithId attrSt)
	{
		Span stDetails = new Span();
		stDetails.setText(attrSt.statement.toString());
		FormLayout wrapper = new FormLayout(stDetails);
		wrapper.setWidth(95, Unit.PERCENTAGE);
		return wrapper;
	}

	private List<SingleActionHandler<AttrStatementWithId>> getGlobalHamburgerActionsHandlers()
	{
		SingleActionHandler<AttrStatementWithId> add = SingleActionHandler
				.builder4Add(msg::getMessage, AttrStatementWithId.class).withHandler(this::showAddDialog).build();

		return Arrays.asList(add, getDeleteAction());
	}

	private List<SingleActionHandler<AttrStatementWithId>> getRowActionsHandlers()
	{
		SingleActionHandler<AttrStatementWithId> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, AttrStatementWithId.class).withHandler(this::showEditDialog).build();

		return Arrays.asList(edit, getDeleteAction());
	}

	private SingleActionHandler<AttrStatementWithId> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg::getMessage, AttrStatementWithId.class)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Set<AttrStatementWithId> items)
	{
		ConfirmDialog confirmDialog = new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AttributeStatements.confirmDelete"),
				msg.getMessage("ok"),
				e -> removeStatements(items),
				msg.getMessage("cancel"),
				e -> {}
		);
		confirmDialog.setWidth("30em");
		confirmDialog.open();
	}

	private void removeStatements(Collection<AttrStatementWithId> removedStatements)
	{
		removedStatements.forEach(attrStatementsGrid::removeElement);
		updateGroup();
	}

	private void updateStatement(AttrStatementWithId oldStatement, AttributeStatement newStatement)
	{
		attrStatementsGrid.replaceElement(oldStatement, new AttrStatementWithId(newStatement));
		updateGroup();
	}

	private void showEditDialog(Collection<AttrStatementWithId> target)
	{
		AttrStatementWithId st = target.iterator().next();
		AttributeStatement old = st.statement.clone();

		controller.getEditStatementDialog(group.toString(), old, s -> updateStatement(st, s)).open();
	}

	private void showAddDialog(Set<AttrStatementWithId> target)
	{
		controller.getEditStatementDialog(group.toString(), null, this::addStatement).open();
	}

	private void addStatement(AttributeStatement newStatement)
	{
		attrStatementsGrid.addElement(new AttrStatementWithId(newStatement));
		updateGroup();
	}

	private void updateGroup()
	{
		controller.updateGroup(attrStatementsGrid.getElements(), group, bus);
	}

	void setInput(Group group)
	{
		this.group = group;
		attrStatementsGrid.setItems(Arrays.stream(group.getAttributeStatements())
				.map(AttrStatementWithId::new)
				.collect(Collectors.toList()));
	}
}
