/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webadmin.Images;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import pl.edu.icm.unity.webadmin.identities.EntityChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Displays attributes and their values. 
 * Allows for adding/removing attributes.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributesPanel extends HorizontalSplitPanel
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	private AttributesManagement attributesManagement;
	
	private VerticalLayout left;
	private List<Attribute<?>> attributes;
	private ValuesRendererPanel attributeValues;
	private Table attributesTable;
	private EntityParam owner;
	private String groupPath;
	private Map<String, AttributeType> attributeTypes;
	
	@Autowired
	public AttributesPanel(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributesManagement attributesManagement)
	{
		this.msg = msg;
		this.registry = registry;
		this.attributesManagement = attributesManagement;
		setStyleName(Reindeer.SPLITPANEL_SMALL);
		attributesTable = new Table();
		attributesTable.setNullSelectionAllowed(false);
		attributesTable.setImmediate(true);
		attributesTable.setSizeFull();
		BeanItemContainer<AttributeItem> tableContainer = new BeanItemContainer<AttributeItem>(AttributeItem.class);
		attributesTable.setSelectable(true);
		attributesTable.setMultiSelect(false);
		attributesTable.setContainerDataSource(tableContainer);
		attributesTable.setColumnHeaders(new String[] {msg.getMessage("Attribute.attributes")});
		attributesTable.addActionHandler(new AddAttributeActionHandler());
		attributesTable.addActionHandler(new EditAttributeActionHandler());
		attributesTable.addActionHandler(new RemoveAttributeActionHandler());
		
		attributesTable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				AttributeItem selected = (AttributeItem)attributesTable.getValue(); 
				if (selected != null)
					updateValues(selected.getAttribute());
				else
					updateValues(null);
			}
		});

		attributeValues = new ValuesRendererPanel(msg);
		attributeValues.setSizeFull();

		left = new VerticalLayout();
		left.setMargin(new MarginInfo(false, true, false, false));
		left.setSizeFull();
		
		setFirstComponent(left);
		setSecondComponent(attributeValues);
		setSplitPosition(40, Unit.PERCENTAGE);
		
		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(new EventListener<EntityChangedEvent>()
		{
			@Override
			public void handleEvent(EntityChangedEvent event)
			{
				setInput(event.getEntity() == null ? null :
					new EntityParam(event.getEntity().getId()), event.getGroup());
			}
		}, EntityChangedEvent.class);
		
		bus.addListener(new EventListener<AttributeTypesUpdatedEvent>()
		{
			@Override
			public void handleEvent(AttributeTypesUpdatedEvent event)
			{
				setAttributeTypes(event.getAttributeTypes());
			}
		}, AttributeTypesUpdatedEvent.class);
		
		setInput(null, "/");
	}
	
	private void setAttributeTypes(List<AttributeType> atList)
	{
		attributeTypes = new HashMap<String, AttributeType>();
		for (AttributeType at: atList)
			attributeTypes.put(at.getName(), at);	
	}
	
	private void setInput(EntityParam owner, String groupPath)
	{
		this.owner = owner;
		
		if (owner == null)
		{
			showLabel(msg.getMessage("Attribute.noEntitySelected"));
			return;
		}
		
		try
		{
			Collection<Attribute<?>> attributesCol = attributesManagement.getAllAttributes(
					owner, groupPath, null);
			this.attributes = new ArrayList<Attribute<?>>(attributesCol.size());
			this.attributes.addAll(attributesCol);
			this.groupPath = groupPath;
			updateAttributes();
			left.removeAllComponents();
			left.addComponent(attributesTable);
		} catch (EngineException e)
		{
			showLabel(msg.getMessage("Attribute.noReadAuthz", groupPath));
		}
		
	}
	
	private void showLabel(String value)
	{
		Label info = new Label(value);
		attributesTable.removeAllItems();
		attributeValues.removeValues();
		left.removeAllComponents();
		left.addComponent(info);
	}
	
	private void updateAttributes()
	{
		attributesTable.removeAllItems();
		attributeValues.removeValues();
		if (attributes.size() == 0)
			return;
		for (Attribute<?> attribute: attributes)
			attributesTable.addItem(new AttributeItem(attribute));

		attributesTable.select(attributes.get(0));
	}
	
	private void updateValues(Attribute<?> attribute)
	{
		if (attribute == null)
		{
			attributeValues.removeValues();
			return;
		}
		AttributeValueSyntax<?> syntax = attribute.getAttributeSyntax();
		WebAttributeHandler<?> handler = registry.getHandler(syntax.getValueSyntaxId());
		attributeValues.setValues(handler, syntax, attribute.getValues());
	}
	
	public class AttributeItem
	{
		private Attribute<?> attribute;

		public AttributeItem(Attribute<?> value)
		{
			this.attribute = value;
		}
		
		public String getName()
		{
			return attribute.getName(); 
		}
		
		private Attribute<?> getAttribute()
		{
			return attribute;
		}
	}
	
	private void removeAttribute(AttributeItem attributeItem)
	{
		Attribute<?> toRemove = attributeItem.getAttribute();
		try
		{
			attributesManagement.removeAttribute(owner, toRemove.getGroupPath(), toRemove.getName());
			attributes.remove(toRemove);
			updateAttributes();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("Attribute.removeAttributeError", toRemove.getName()), e);
		}
	}
	
	private boolean addAttribute(Attribute<?> attribute)
	{
		try
		{
			attributesManagement.setAttribute(owner, attribute, false);
			attributes.add(attribute);
			updateAttributes();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("Attribute.addAttributeError", attribute.getName()), e);
			return false;
		}
	}

	private boolean updateAttribute(Attribute<?> attribute)
	{
		try
		{
			attributesManagement.setAttribute(owner, attribute, true);
			for (int i=0; i<attributes.size(); i++)
			{
				if (attributes.get(i).getName().equals(attribute.getName()))
				{
					attributes.set(i, attribute);
					System.out.println("Update at " + i);
				}
					
			}
			updateAttributes();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("Attribute.addAttributeError", attribute.getName()), e);
			return false;
		}
	}
	
	private class RemoveAttributeActionHandler extends SingleActionHandler
	{
		public RemoveAttributeActionHandler()
		{
			super(msg.getMessage("Attribute.removeAttribute"), 
					Images.delete.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			ConfirmDialog confirm = new ConfirmDialog(msg, msg.getMessage("Attribute.removeConfirm", 
					((AttributeItem) target).getAttribute().getName()), 
					new Callback()
					{
						@Override
						public void onConfirm()
						{
							removeAttribute((AttributeItem) target);
						}
					});
			confirm.show();
		}
	}

	private class AddAttributeActionHandler extends SingleActionHandler
	{
		public AddAttributeActionHandler()
		{
			super(msg.getMessage("Attribute.addAttribute"), 
					Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			AttributeEditor attributeEditor = new AttributeEditor(msg, attributeTypes.values(), 
					groupPath, registry);
			AttributeEditDialog dialog = new AttributeEditDialog(msg, 
					msg.getMessage("Attribute.addAttribute"), 
					new AttributeEditDialog.Callback()
					{
						@Override
						public boolean newAttribute(Attribute<?> newAttribute)
						{
							return addAttribute(newAttribute);
						}
					}, attributeEditor);
			dialog.show();
		}
	}

	private class EditAttributeActionHandler extends SingleActionHandler
	{
		public EditAttributeActionHandler()
		{
			super(msg.getMessage("Attribute.editAttribute"), 
					Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			Attribute<?> attribute = ((AttributeItem) target).getAttribute();
			AttributeType attributeType = attributeTypes.get(attribute.getName());
			AttributeEditor attributeEditor = new AttributeEditor(msg, attributeType, attribute, 
					registry);
			AttributeEditDialog dialog = new AttributeEditDialog(msg, 
					msg.getMessage("Attribute.addAttribute"), 
					new AttributeEditDialog.Callback()
					{
						@Override
						public boolean newAttribute(Attribute<?> newAttribute)
						{
							return updateAttribute(newAttribute);
						}
					}, attributeEditor);
			dialog.show();
		}
	}

}
