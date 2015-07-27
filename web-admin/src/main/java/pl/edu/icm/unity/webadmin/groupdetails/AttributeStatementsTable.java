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
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementWebHandlerFactory;
import pl.edu.icm.unity.webadmin.attrstmt.StatementHandlersRegistry;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.groupdetails.AttributeStatementEditDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallTable;

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
	private StatementHandlersRegistry statementHandlersReg;
	private Group group;
	private EventsBus bus;
	private List<SingleActionHandler> actionHandlers;
	
	
	public AttributeStatementsTable(UnityMessageSource msg, GroupsManagement groupsMan,
			AttributesManagement attrsMan, 
			StatementHandlersRegistry statementHandlersRegistry)
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.attrsMan = attrsMan;
		this.statementHandlersReg = statementHandlersRegistry;
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
		AttributeStatement[] ases = group.getAttributeStatements();
		for (AttributeStatement as: ases)
		{
			AttributeStatementWebHandlerFactory handler = statementHandlersReg.getHandler(as.getName());
			addItem(new Object[] {handler.getTextRepresentation(as)}, as);
		}
	}	
	
	private void updateGroup(AttributeStatement[] attributeStatements)
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
	
	private void removeStatements(Collection<AttributeStatement> removedStatements)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		AttributeStatement[] attributeStatements = new AttributeStatement[items.size()-removedStatements.size()];
		for (int i=0; it.hasNext(); i++)
		{
			AttributeStatement s = (AttributeStatement) it.next();
			boolean check = false;
			for (AttributeStatement st : removedStatements)
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

	private void addStatement(AttributeStatement newStatement)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		AttributeStatement[] attributeStatements = new AttributeStatement[items.size()+1];
		for (int i=0; it.hasNext(); i++)
		{
			AttributeStatement s = (AttributeStatement) it.next();
			attributeStatements[i] = s;
		}
		attributeStatements[items.size()] = newStatement;
		
		updateGroup(attributeStatements);
	}

	private void updateStatement(AttributeStatement oldStatement, AttributeStatement newStatement)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		AttributeStatement[] attributeStatements = new AttributeStatement[items.size()];
		for (int i=0; it.hasNext(); i++)
		{
			AttributeStatement s = (AttributeStatement) it.next();
			if (!oldStatement.equals(s))
				attributeStatements[i] = s;
			else
				attributeStatements[i] = newStatement;
		}
		
		updateGroup(attributeStatements);
	}

	private void moveItemAfter(AttributeStatement toMoveItemId, AttributeStatement moveAfterItemId)
	{
		Collection<?> items = getItemIds();
		Iterator<?> it = items.iterator();
		List<AttributeStatement> attributeStatements = new ArrayList<AttributeStatement>(items.size());
		if (moveAfterItemId == null)
			attributeStatements.add(toMoveItemId);
		while (it.hasNext())
		{
			AttributeStatement s = (AttributeStatement) it.next();
			
			if (!s.equals(toMoveItemId))
			{
				attributeStatements.add(s);
			} 
			
			if (s.equals(moveAfterItemId))
			{
				attributeStatements.add(toMoveItemId);
			}
		}
		AttributeStatement[] aStmtsA = attributeStatements.toArray(
				new AttributeStatement[attributeStatements.size()]);
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
				moveItemAfter((AttributeStatement)sourceItemId, (AttributeStatement) previous);
			} else if (location == VerticalDropLocation.BOTTOM) 
			{
				moveItemAfter((AttributeStatement)sourceItemId, (AttributeStatement) targetItemId);
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
			
			final Collection<AttributeStatement> items = new ArrayList<AttributeStatement>();
			Collection<?> ats = (Collection<?>) target;
			for (Object o: ats)
			{
				items.add((AttributeStatement) o);
			}
					
			new ConfirmDialog(msg, msg.getMessage("AttributeStatements.confirmDelete"),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					
					removeStatements(items);
					
				}
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
			new AttributeStatementEditDialog(msg, null, attrsMan, statementHandlersReg, group.toString(),
					new Callback()
					{
						@Override
						public void onConfirm(AttributeStatement newStatement)
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
			
			AttributeStatement st = (AttributeStatement) target;
			new AttributeStatementEditDialog(msg, st, 
					attrsMan, statementHandlersReg, group.toString(), new Callback()
					{
						@Override
						public void onConfirm(AttributeStatement newStatement)
						{
							updateStatement((AttributeStatement) target, newStatement);
						}
					}).show();
		}
	}

}
