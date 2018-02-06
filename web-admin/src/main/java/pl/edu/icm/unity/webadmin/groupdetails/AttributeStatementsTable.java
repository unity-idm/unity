/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.components.grid.GridDragSource;
import com.vaadin.ui.components.grid.GridDropTarget;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementEditDialog;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.DnDGridUtils;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Table with attribute statements. Allows for management operations.
 * @author K. Benedyczak
 */
public class AttributeStatementsTable extends GenericElementsTable<AttributeStatement>
{
	private final String DND_TYPE = "attribute_statement";
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private AttributeTypeManagement attrsMan;
	private Group group;
	private EventsBus bus;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	
	public AttributeStatementsTable(UnityMessageSource msg, GroupsManagement groupsMan,
			AttributeTypeManagement attrsMan,
			AttributeHandlerRegistry attributeHandlerRegistry)
	{

		super(msg.getMessage("AttributeStatements.tableHdr"), a -> a.toString(), false);
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.attrsMan = attrsMan;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.bus = WebSession.getCurrent().getEventBus();
		setMultiSelect(false);
		
		addActionHandler(getAddAction());
		addActionHandler(getEditAction());
		addActionHandler(getDeleteAction());

		
		GridDragSource<AttributeStatement> source = new GridDragSource<>(this);
		source.setDragDataGenerator(DND_TYPE, as -> "{}");
		source.addGridDragStartListener(e -> source.setDragData(e.getDraggedItems().iterator().next()));
		source.addGridDragEndListener(e -> source.setDragData(null));

		GridDropTarget<AttributeStatement> target = new GridDropTarget<>(this,
				DropMode.ON_TOP_OR_BETWEEN);
		target.setDropCriteriaScript(DnDGridUtils.getTypedCriteriaScript(DND_TYPE));
		target.addGridDropListener(e -> 
		{
			Optional<Object> dragData = e.getDragData();
			if (!dragData.isPresent() || e.getDropTargetRow() == null 
					|| e.getDropTargetRow().get() == null)
				return;
			int index = contents.indexOf(e.getDropTargetRow().get());
			AttributeStatement attrS = (AttributeStatement) dragData.get();
			contents.remove(attrS);
			contents.add(index, attrS);
			updateGroup();
		});
	}

	public void setInput(Group group)
	{
		this.group = group;
		super.setInput(Arrays.asList(group.getAttributeStatements()));
	}

	private void updateGroup()
	{
		AttributeStatement[] attributeStatements = contents
				.toArray(new AttributeStatement[contents.size()]);
		Group updated = group.clone();
		updated.setAttributeStatements(attributeStatements);
		try
		{
			groupsMan.updateGroup(updated.toString(), updated);
			bus.fireEvent(new GroupChangedEvent(group.toString()));
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("AttributeStatements.cantUpdateGroup"), e);
		}
	}

	private boolean checkExists(AttributeStatement toCheck)
	{
		for (AttributeStatement a : contents)
			if (a.equals(toCheck))
				return true;
		return false;
	}

	private SingleActionHandler<AttributeStatement> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, AttributeStatement.class)
				.withHandler(this::showAddDialog).build();
	}

	private void showAddDialog(Set<AttributeStatement> target)
	{

		new AttributeStatementEditDialog(msg, null, attrsMan, group.toString(),
				attributeHandlerRegistry, groupsMan, st -> addStatement(st)).show();
	}

	private void addStatement(AttributeStatement newStatement)
	{
		if (!checkExists(newStatement))
			addElement(newStatement);
		updateGroup();
	}

	private SingleActionHandler<AttributeStatement> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, AttributeStatement.class)
				.withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<AttributeStatement> target)
	{
		AttributeStatement st = target.iterator().next();
		AttributeStatement old = st.clone();
		new AttributeStatementEditDialog(msg, old, attrsMan, group.toString(),
				attributeHandlerRegistry, groupsMan,
				newSt -> updateStatement(st, newSt)).show();
	}

	private void updateStatement(AttributeStatement oldStatement,
			AttributeStatement newStatement)
	{

		int index = contents.indexOf(oldStatement);
		contents.remove(oldStatement);
		if (!checkExists(newStatement))
		{
			contents.add(index, newStatement);
		}
		updateGroup();
	}

	private SingleActionHandler<AttributeStatement> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, AttributeStatement.class)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Set<AttributeStatement> items)
	{
		new ConfirmDialog(msg, msg.getMessage("AttributeStatements.confirmDelete"), () -> {
			removeStatements(items);
		}).show();
	}

	private void removeStatements(Collection<AttributeStatement> removedStatements)
	{
		removedStatements.forEach(this::removeElement);
		updateGroup();
	}
}
