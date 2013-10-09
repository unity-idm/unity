/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypeEditDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.Action;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for attribute types management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributeTypesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributesManagement attrManagement;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private AttributeMetadataHandlerRegistry attrMetaHandlerRegistry;
	
	private GenericElementsTable<AttributeType> table;
	private AttributeTypeViewer viewer;
	private com.vaadin.ui.Component main;
	private EventsBus bus;
	
	
	@Autowired
	public AttributeTypesComponent(UnityMessageSource msg, AttributesManagement attrManagement, 
			AttributeHandlerRegistry attrHandlerRegistry, 
			AttributeMetadataHandlerRegistry attrMetaHandlerRegistry)
	{
		this.msg = msg;
		this.attrManagement = attrManagement;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.attrMetaHandlerRegistry = attrMetaHandlerRegistry;
		this.bus = WebSession.getCurrent().getEventBus();
		HorizontalLayout hl = new HorizontalLayout();
		
		setCaption(msg.getMessage("AttributeTypes.caption"));
		table = new GenericElementsTable<AttributeType>(msg.getMessage("AttributeTypes.types"), 
				AttributeType.class, new GenericElementsTable.NameProvider<AttributeType>()
				{
					@Override
					public Label toRepresentation(AttributeType element)
					{
						Label ret = new Label(element.getName());
						if (element.isTypeImmutable())
							ret.addStyleName(Styles.gray.toString());
						return ret;
					}
				});
		table.setWidth(90, Unit.PERCENTAGE);
		hl.addComponent(table);
		viewer = new AttributeTypeViewer(msg);
		hl.addComponent(viewer);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				@SuppressWarnings("unchecked")
				GenericItem<AttributeType> item = (GenericItem<AttributeType>)table.getValue();
				if (item != null)
				{
					AttributeType at = item.getElement();
					WebAttributeHandler<?> handler = AttributeTypesComponent.this.attrHandlerRegistry.getHandler(
							at.getValueType().getValueSyntaxId());
					viewer.setInput(at, handler, AttributeTypesComponent.this.attrMetaHandlerRegistry);
				} else
					viewer.setInput(null, null, AttributeTypesComponent.this.attrMetaHandlerRegistry);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		refresh();
	}
	
	public void refresh()
	{
		try
		{
			Collection<AttributeType> types = attrManagement.getAttributeTypes();
			table.setInput(types);
			removeAllComponents();
			addComponent(main);
			bus.fireEvent(new AttributeTypesUpdatedEvent(types));
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("AttributeTypes.errorGetTypes"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}
	
	private boolean updateType(AttributeType type)
	{
		try
		{
			attrManagement.updateAttributeType(type);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("AttributeTypes.errorUpdate"), e);
			return false;
		}
	}

	private boolean addType(AttributeType type)
	{
		try
		{
			attrManagement.addAttributeType(type);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("AttributeTypes.errorAdd"), e);
			return false;
		}
	}

	private boolean removeType(String name, boolean withInstances)
	{
		try
		{
			attrManagement.removeAttributeType(name, withInstances);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("AttributeTypes.errorRemove"), e);
			return false;
		}
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("AttributeTypes.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}

	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("AttributeTypes.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			AttributeTypeEditor editor = new AttributeTypeEditor(msg, attrHandlerRegistry, 
					attrMetaHandlerRegistry);
			AttributeTypeEditDialog dialog = new AttributeTypeEditDialog(msg, 
					msg.getMessage("AttributeTypes.addAction"), new Callback()
					{
						@Override
						public boolean newAttribute(AttributeType newAttributeType)
						{
							return addType(newAttributeType);
						}
					}, editor);
			dialog.show();
		}
	}
	
	/**
	 * Extends {@link SingleActionHandler}. Returns action only for selections on mutable attribute type items. 
	 * @author K. Benedyczak
	 */
	private abstract class AbstractAttributeTypeActionHandler extends SingleActionHandler
	{

		public AbstractAttributeTypeActionHandler(String caption, Resource icon)
		{
			super(caption, icon);
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null || !(target instanceof GenericItem))
				return EMPTY;
			@SuppressWarnings("unchecked")
			GenericItem<AttributeType> item = (GenericItem<AttributeType>)target;
			final AttributeType at = item.getElement();
			if (at.isTypeImmutable())
				return EMPTY;
			return super.getActions(target, sender);
		}
	}

	
	private class EditActionHandler extends AbstractAttributeTypeActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("AttributeTypes.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<AttributeType> item = (GenericItem<AttributeType>)target;
			AttributeType at = item.getElement();
			AttributeTypeEditor editor = new AttributeTypeEditor(msg, attrHandlerRegistry, at,
					attrMetaHandlerRegistry);
			AttributeTypeEditDialog dialog = new AttributeTypeEditDialog(msg, 
					msg.getMessage("AttributeTypes.editAction"), new Callback()
					{
						@Override
						public boolean newAttribute(AttributeType newAttributeType)
						{
							return updateType(newAttributeType);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class DeleteActionHandler extends AbstractAttributeTypeActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("AttributeTypes.deleteAction"), 
					Images.delete.getResource());
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<AttributeType> item = (GenericItem<AttributeType>)target;
			final AttributeType at = item.getElement();
			new ConfirmWithOptionDialog(msg, msg.getMessage("AttributeTypes.confirmDelete", at.getName()),
					msg.getMessage("AttributeTypes.withInstances"),
					new ConfirmWithOptionDialog.Callback()
			{
				@Override
				public void onConfirm(boolean withInstances)
				{
					removeType(at.getName(), withInstances);
				}
			}).show();
		}
	}
}
