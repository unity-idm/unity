/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webadmin.attribute.ValueEditDialog.Callback;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.Not;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.AbstractSelect.VerticalLocationIs;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.Table.TableTransferable;

/**
 * Displays an editable table with attribute values. Allows for reordering, removing values, 
 * editing them and adding new ones.
 * @author K. Benedyczak
 */
public class ValuesEditorPanel<T> extends VerticalLayout
{
	private UnityMessageSource msg;
	
	private AttributeValueSyntax<T> syntax;
	private WebAttributeHandler<T> handler;
	
	private ValuesTable valuesTable;
	private String attributeName;
	
	
	public ValuesEditorPanel(UnityMessageSource msg, 
			List<T> values, AttributeValueSyntax<T> syntax, WebAttributeHandler<T> handler)
	{
		this.msg = msg;
		this.syntax = syntax;
		this.handler = handler;
		valuesTable = new ValuesTable(msg);
		valuesTable.setDragMode(TableDragMode.ROW);
		valuesTable.setDropHandler(new DropHandlerImpl());
		valuesTable.addActionHandler(new RemoveActionHandler());
		valuesTable.addActionHandler(new AddActionHandler());
		valuesTable.addActionHandler(new EditActionHandler());
		addComponent(valuesTable);
		setMargin(true);
		valuesTable.setValues(values, syntax, handler);
		valuesTable.setSizeFull();
		setSizeFull();
	}

	
	public List<?> getValues()
	{
		return valuesTable.getValues();
	}
	
	public void setValues(List<T> values, String attributeName)
	{
		valuesTable.setValues(values, syntax, handler);
		this.attributeName = attributeName;
	}
	
	private class DropHandlerImpl implements DropHandler
	{
		@Override
		public void drop(DragAndDropEvent event)
		{
			TableTransferable t = (TableTransferable) event.getTransferable();
			if (t.getSourceComponent() != valuesTable)
				return;

			AbstractSelectTargetDetails target = (AbstractSelectTargetDetails) event.getTargetDetails();
			Object sourceItemId = t.getItemId();
			Object targetItemId = target.getItemIdOver();
	                VerticalDropLocation location = target.getDropLocation();

			if (sourceItemId == targetItemId)
				return;

			if (location == VerticalDropLocation.TOP) 
			{
				valuesTable.moveBefore(sourceItemId, targetItemId);
			} else if (location == VerticalDropLocation.BOTTOM) 
			{
				valuesTable.moveItemAfter(sourceItemId, targetItemId);
			}
		}

		@Override
		public AcceptCriterion getAcceptCriterion()
		{
			return new Not(VerticalLocationIs.MIDDLE);
		}
	}

	private class RemoveActionHandler extends SingleActionHandler
	{
		public RemoveActionHandler()
		{
			super(msg.getMessage("Attribute.removeValue"), 
					Images.delete.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			valuesTable.removeItem(target);
		}
	}
	
	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("Attribute.editValue"), 
					Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			final T value = (T) valuesTable.getItemById(target);
			AttributeValueEditor<T> editor = handler.getEditorComponent(value, attributeName, syntax);
			ValueEditDialog<T> dialog = new ValueEditDialog<T>(msg, msg.getMessage("Attribute.editValue"), 
					editor, new Callback<T>()
					{
						@Override
						public void updateValue(T newValue)
						{
							valuesTable.updateItem(target, newValue);
						}
					});
			dialog.show();
			
		}
	}
	
	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("Attribute.addValue"), 
					Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			AttributeValueEditor<T> editor = handler.getEditorComponent(null, attributeName, syntax);
			ValueEditDialog<T> dialog = new ValueEditDialog<T>(msg, msg.getMessage("Attribute.addValue"), 
					editor, new Callback<T>()
					{
						@Override
						public void updateValue(T newValue)
						{
							valuesTable.addItem(target, newValue);
						}
					});
			dialog.show();
		}
	}

}
