/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementEditDialog;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementEditDialog.Callback;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallTable;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.data.Container;
import com.vaadin.event.Action;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.Not;
import com.vaadin.shared.ui.dd.VerticalDropLocation;

/**
 * Table with attribute statements. Allows for management operations.
 * @author K. Benedyczak
 */
public class AttributeStatementsTable extends SmallTable
{
	private static final String MAIN_COL = "main";
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private AttributesManagement attrsMan;
	private Group group;
	private EventsBus bus;
	private List<SingleActionHandler> actionHandlers;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	
	
	public AttributeStatementsTable(UnityMessageSource msg, GroupsManagement groupsMan,
			AttributesManagement attrsMan, 
			AttributeHandlerRegistry attributeHandlerRegistry)
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.attrsMan = attrsMan;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.bus = WebSession.getCurrent().getEventBus();
		this.actionHandlers = new ArrayList<>();
		
		addContainerProperty(MAIN_COL, String.class, null);
		setColumnHeader(MAIN_COL, msg.getMessage("AttributeStatements.tableHdr"));
		setSizeFull();
		setSortEnabled(false);
		setSelectable(true);
		setMultiSelect(true);
		setImmediate(true);
		addActionHandler(new AddHandler());
		addActionHandler(new EditHandler());
		addActionHandler(new DeleteHandler());
		setDragMode(TableDragMode.ROW);
		setDropHandler(new DropHandlerImpl());
	}
	
	public void setInput(Group group)
	{
		this.group = group;
		removeAllItems();
		AttributeStatement2[] ases = group.getAttributeStatements();
		for (AttributeStatement2 as: ases)
			addItem(new Object[] {as.toString()}, as);
	}	
	
	private void updateGroup(AttributeStatement2[] attributeStatements)
	{
		Group updated = group.clone();
		updated.setAttributeStatements(attributeStatements);
		try
		{
			groupsMan.updateGroup(updated.toString(), updated);
			bus.fireEvent(new GroupChangedEvent(group.toString()));
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributeStatements.cantUpdateGroup"), e);
		}
	}
	
	private void removeStatements(Collection<AttributeStatement2> removedStatements)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		AttributeStatement2[] attributeStatements = new AttributeStatement2
				[items.size()-removedStatements.size()];
		for (int i=0; it.hasNext(); i++)
		{
			AttributeStatement2 s = (AttributeStatement2) it.next();
			boolean check = false;
			for (AttributeStatement2 st : removedStatements)
			{
				if (st.equals(s))
				{
					check = true;
					break;
				}					
			}
			if (!check)
			{
				attributeStatements[i] = s;
			} else
			{
				i--;
			}
		}
		updateGroup(attributeStatements);
	}

	private void addStatement(AttributeStatement2 newStatement)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		AttributeStatement2[] attributeStatements = new AttributeStatement2[items.size()+1];
		for (int i=0; it.hasNext(); i++)
		{
			AttributeStatement2 s = (AttributeStatement2) it.next();
			attributeStatements[i] = s;
		}
		attributeStatements[items.size()] = newStatement;
		
		updateGroup(attributeStatements);
	}

	private void updateStatement(AttributeStatement2 oldStatement, AttributeStatement2 newStatement)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		AttributeStatement2[] attributeStatements = new AttributeStatement2[items.size()];
		for (int i=0; it.hasNext(); i++)
		{
			AttributeStatement2 s = (AttributeStatement2) it.next();
			if (!oldStatement.equals(s))
				attributeStatements[i] = s;
			else
				attributeStatements[i] = newStatement;
		}
		
		updateGroup(attributeStatements);
	}

	private void moveItemAfter(AttributeStatement2 toMoveItemId, AttributeStatement2 moveAfterItemId)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		List<AttributeStatement2> attributeStatements = new ArrayList<AttributeStatement2>(items.size());
		if (moveAfterItemId == null)
			attributeStatements.add(toMoveItemId);
		while (it.hasNext())
		{
			AttributeStatement2 s = (AttributeStatement2) it.next();
			
			if (!s.equals(toMoveItemId))
			{
				attributeStatements.add(s);
			} 
			
			if (s.equals(moveAfterItemId))
			{
				attributeStatements.add(toMoveItemId);
			}
		}
		AttributeStatement2[] aStmtsA = attributeStatements.toArray(
				new AttributeStatement2[attributeStatements.size()]);
		updateGroup(aStmtsA);
	}
	
	
	private class DropHandlerImpl implements DropHandler
	{
		@Override
		public void drop(DragAndDropEvent event)
		{
			TableTransferable t = (TableTransferable) event.getTransferable();
			if (t.getSourceComponent() != AttributeStatementsTable.this)
				return;

			AbstractSelectTargetDetails target = (AbstractSelectTargetDetails) event.getTargetDetails();
			Object sourceItemId = t.getItemId();
			Object targetItemId = target.getItemIdOver();
	                VerticalDropLocation location = target.getDropLocation();

			if (sourceItemId == targetItemId)
				return;

			if (location == VerticalDropLocation.TOP) 
			{
				Container.Ordered container = (Container.Ordered)getContainerDataSource();
				Object previous = container.prevItemId(targetItemId);
				if (sourceItemId == previous)
					return;
				moveItemAfter((AttributeStatement2)sourceItemId, (AttributeStatement2) previous);
			} else if (location == VerticalDropLocation.BOTTOM) 
			{
				moveItemAfter((AttributeStatement2)sourceItemId, (AttributeStatement2) targetItemId);
			}
		}

		@Override
		public AcceptCriterion getAcceptCriterion()
		{
			return new Not(VerticalLocationIs.MIDDLE);
		}
	}

	@Override
	public void addActionHandler(Action.Handler actionHandler) {
		super.addActionHandler(actionHandler);
		if (actionHandler instanceof SingleActionHandler)
			actionHandlers.add((SingleActionHandler) actionHandler);
	}

	public List<SingleActionHandler> getActionHandlers()
	{
		return actionHandlers;
	}
	
	private class DeleteHandler extends SingleActionHandler
	{
		public DeleteHandler()
		{
			super(msg.getMessage("AttributeStatements.removeStatement"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			final Collection<AttributeStatement2> items = new ArrayList<AttributeStatement2>();
			Collection<?> ats = (Collection<?>) target;
			for (Object o: ats)
				items.add((AttributeStatement2) o);
					
			new ConfirmDialog(msg, msg.getMessage("AttributeStatements.confirmDelete"), () -> {
				removeStatements(items);
			}).show();
		}
	}

	private class AddHandler extends SingleActionHandler
	{
		public AddHandler()
		{
			super(msg.getMessage("AttributeStatements.addStatement"), 
					Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			new AttributeStatementEditDialog(msg, null, attrsMan, group.toString(),
					attributeHandlerRegistry, groupsMan, new Callback()
					{
						@Override
						public void onConfirm(AttributeStatement2 newStatement)
						{
							addStatement(newStatement);
						}
					}).show();
		}
	}

	private class EditHandler extends SingleActionHandler
	{
		public EditHandler()
		{
			super(msg.getMessage("AttributeStatements.editStatement"), 
					Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			
			AttributeStatement2 st = (AttributeStatement2) target;
			new AttributeStatementEditDialog(msg, st, attrsMan, group.toString(), 
					attributeHandlerRegistry, groupsMan, new Callback()
					{
						@Override
						public void onConfirm(AttributeStatement2 newStatement)
						{
							updateStatement((AttributeStatement2) target, newStatement);
						}
					}).show();
		}
	}

}
