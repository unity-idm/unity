/*

 *Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser.groupdetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.shared.ui.Orientation;
import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.GridDragSource;
import com.vaadin.ui.components.grid.GridDropTarget;

import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.DnDGridUtils;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Allows manage attribute statements
 * @author P.Piernik
 *
 */
class AttributeStatementsComponent extends CustomComponent
{
	private final String DND_TYPE = "attribute_statement";

	private MessageSource msg;
	private AttributeStatementController controller;
	private EventsBus bus;
	private GridWithActionColumn<AttrStatementWithId> attrStatementsGrid;
	private ComponentWithToolbar attrStatementsGridWithToolbar;

	private Group group;

	AttributeStatementsComponent(MessageSource msg, AttributeStatementController controller)
	{
		this.msg = msg;
		this.controller = controller;
		this.bus = WebSession.getCurrent().getEventBus();

		attrStatementsGrid = new GridWithActionColumn<>(msg, Collections.emptyList(), false, false);
		attrStatementsGrid.addHamburgerActions(getRowActionsHandlers());
		attrStatementsGrid.addShowDetailsColumn(s -> getDetailsComponent(s));

		attrStatementsGrid.addColumn(s -> s.toShortString(), msg.getMessage("AttributeStatements.nameCaption"),
				90).setResizable(true);
		attrStatementsGrid.setSizeFull();
		attrStatementsGrid.setMultiSelect(true);

		GridDragSource<AttrStatementWithId> source = new GridDragSource<>(attrStatementsGrid);
		source.setDragDataGenerator(DND_TYPE, as -> "{}");
		source.addGridDragStartListener(e -> source.setDragData(e.getDraggedItems().iterator().next()));
		source.addGridDragEndListener(e -> source.setDragData(null));

		GridDropTarget<AttrStatementWithId> target = new GridDropTarget<>(attrStatementsGrid,
				DropMode.ON_TOP_OR_BETWEEN);
		target.setDropCriteriaScript(DnDGridUtils.getTypedCriteriaScript(DND_TYPE));
		target.addGridDropListener(e -> {
			Optional<Object> dragData = e.getDragData();
			if (!dragData.isPresent() || e.getDropTargetRow() == null || e.getDropTargetRow().get() == null)
				return;
			int index = attrStatementsGrid.getElements().indexOf(e.getDropTargetRow().get());
			AttrStatementWithId attrS = (AttrStatementWithId) dragData.get();
			attrStatementsGrid.removeElement(attrS);
			attrStatementsGrid.addElement(index, attrS);
			updateGroup();
		});

		HamburgerMenu<AttrStatementWithId> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addActionHandlers(getGlobalHamburgerActionsHandlers());
		attrStatementsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		Toolbar<AttrStatementWithId> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.addStyleName("u-button-height-toolbar");
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(hamburgerMenu);
		attrStatementsGridWithToolbar = new ComponentWithToolbar(attrStatementsGrid, toolbar,
				Alignment.BOTTOM_LEFT);
		attrStatementsGridWithToolbar.setSizeFull();
		attrStatementsGridWithToolbar.setSpacing(true);

		VerticalLayout main = new VerticalLayout();
		main.addComponent(attrStatementsGridWithToolbar);
		main.setSizeFull();
		main.setMargin(false);
		setSizeFull();
		setCompositionRoot(main);
	}

	private FormLayout getDetailsComponent(AttrStatementWithId attrSt)
	{
		Label stDetails = new Label();
		stDetails.setValue(attrSt.statement.toString());
		FormLayout wrapper = new FormLayout(stDetails);
		stDetails.setStyleName(Styles.wordWrap.toString());
		wrapper.setWidth(95, Unit.PERCENTAGE);
		return wrapper;
	}

	private List<SingleActionHandler<AttrStatementWithId>> getGlobalHamburgerActionsHandlers()
	{
		SingleActionHandler<AttrStatementWithId> add = SingleActionHandler
				.builder4Add(msg, AttrStatementWithId.class).withHandler(this::showAddDialog).build();

		return Arrays.asList(add, getDeleteAction());
	}

	private List<SingleActionHandler<AttrStatementWithId>> getRowActionsHandlers()
	{
		SingleActionHandler<AttrStatementWithId> edit = SingleActionHandler
				.builder4Edit(msg, AttrStatementWithId.class).withHandler(this::showEditDialog).build();

		return Arrays.asList(edit, getDeleteAction());
	}

	private SingleActionHandler<AttrStatementWithId> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, AttrStatementWithId.class)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Set<AttrStatementWithId> items)
	{
		new ConfirmDialog(msg, msg.getMessage("AttributeStatements.confirmDelete"), () -> {
			removeStatements(items);
		}).show();
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
		try
		{
			controller.getEditStatementDialog(group.toString(), old, s -> updateStatement(st, s)).show();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void showAddDialog(Set<AttrStatementWithId> target)
	{
		try
		{
			controller.getEditStatementDialog(group.toString(), null, s -> addStatement(s)).show();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void addStatement(AttributeStatement newStatement)
	{
		attrStatementsGrid.addElement(new AttrStatementWithId(newStatement));
		updateGroup();
	}

	private void updateGroup()
	{
		try
		{
			controller.updateGroup(attrStatementsGrid.getElements(), group, bus);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}

	}

	void setInput(Group group)
	{
		this.group = group;
		List<AttrStatementWithId> statemets = Arrays.stream(group.getAttributeStatements())
				.map(AttrStatementWithId::new).collect(Collectors.toList());
		attrStatementsGrid.setItems(statemets);
	}
}
