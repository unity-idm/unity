/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.Action;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypeEditDialog.Callback;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;

/**
 * Responsible for attribute types management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributeTypesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributeTypeManagement attrManagement;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private AttributeMetadataHandlerRegistry attrMetaHandlerRegistry;
	
	private GenericElementsTable<AttributeType> table;
	private AttributeTypeViewer viewer;
	private com.vaadin.ui.Component main;
	private EventsBus bus;
	private AttributeTypeSupport atSupport;
	private UnityServerConfiguration serverConfig;
	private ApplicationContext appContext;
	
	
	@Autowired
	public AttributeTypesComponent(UnityMessageSource msg, AttributeTypeManagement attrManagement,
			AttributeTypeSupport atSupport, 
			AttributeHandlerRegistry attrHandlerRegistry, 
			AttributeMetadataHandlerRegistry attrMetaHandlerRegistry,
			UnityServerConfiguration serverConfig, ApplicationContext appContext)
	{
		this.msg = msg;
		this.attrManagement = attrManagement;
		this.atSupport = atSupport;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.attrMetaHandlerRegistry = attrMetaHandlerRegistry;
		this.bus = WebSession.getCurrent().getEventBus();
		this.serverConfig = serverConfig;
		this.appContext = appContext;
		HorizontalLayout hl = new HorizontalLayout();
		
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("AttributeTypes.caption"));
		table = new GenericElementsTable<AttributeType>(msg.getMessage("AttributeTypes.types"), 
				new GenericElementsTable.NameProvider<AttributeType>()
				{
					@Override
					public Label toRepresentation(AttributeType element)
					{
						Label ret = new Label(element.getName());
						if (element.isTypeImmutable())
							ret.addStyleName(Styles.immutableAttribute.toString());
						return ret;
					}
				});

		viewer = new AttributeTypeViewer(msg);
		table.setMultiSelect(true);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<AttributeType> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null, null, 
							AttributeTypesComponent.this.attrMetaHandlerRegistry);
					return;		
				}	
				AttributeType at = items.iterator().next();	
				if (at != null)
				{
					AttributeValueSyntax<?> syntax = atSupport.getSyntax(at);
					WebAttributeHandler handler = 
							AttributeTypesComponent.this.attrHandlerRegistry.getHandler(syntax);
					viewer.setInput(at, handler, 
							AttributeTypesComponent.this.attrMetaHandlerRegistry);
				} else
					viewer.setInput(null, null, 
							AttributeTypesComponent.this.attrMetaHandlerRegistry);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new ImportActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.addActionHandler(new ExportActionHandler());
		
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);

		hl.addComponents(tableWithToolbar, viewer);
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
			NotificationPopup.showError(msg, msg.getMessage("AttributeTypes.errorUpdate"), e);
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
			NotificationPopup.showError(msg, msg.getMessage("AttributeTypes.errorAdd"), e);
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
			NotificationPopup.showError(msg, msg.getMessage("AttributeTypes.errorRemove"), e);
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
			RegularAttributeTypeEditor editor = new RegularAttributeTypeEditor(msg, attrHandlerRegistry, 
					attrMetaHandlerRegistry, atSupport);
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
	
	private class ImportActionHandler extends SingleActionHandler
	{
		public ImportActionHandler()
		{
			super(msg.getMessage("AttributeTypes.importAction"), Images.transfer.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			
			ImportAttributeTypeDialog dialog = new ImportAttributeTypeDialog(msg,
					msg.getMessage("AttributeTypes.importAction"), serverConfig,
					appContext, attrManagement, () -> {
						refresh();
					});

			dialog.show();
		}
	}
		
	private Collection<AttributeType> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<AttributeType> items = new ArrayList<AttributeType>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			AttributeType at = (AttributeType) i.getElement();
			items.add(at);	
		}
		return items;
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
			if (target == null)
				return EMPTY;
				
			if (target instanceof Collection<?>)
			{
				for (AttributeType item : getItems(target))
				{
					if (item.isTypeImmutable())
						return EMPTY;
				}
			} else
			{
				
				GenericItem<?> item = (GenericItem<?>) target;	
				AttributeType at = (AttributeType) item.getElement();
				if (at.isTypeImmutable())
					return EMPTY;
			}
			return super.getActions(target, sender);
		}
	}

	
	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("AttributeTypes.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			
			GenericItem<?> item = (GenericItem<?>) target;	
			AttributeType at = (AttributeType) item.getElement();
			AttributeTypeEditor editor = at.isTypeImmutable() ? 
					new ImmutableAttributeTypeEditor(msg, at) : 
					new RegularAttributeTypeEditor(msg, attrHandlerRegistry, at, 
							attrMetaHandlerRegistry, atSupport);
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
			setMultiTarget(true);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{	
			final Collection<AttributeType> items = getItems(target);
			String confirmText = MessageUtils.createConfirmFromNames(msg, items);
			new ConfirmWithOptionDialog(msg, msg.getMessage(
					"AttributeTypes.confirmDelete", confirmText),
					msg.getMessage("AttributeTypes.withInstances"),
					new ConfirmWithOptionDialog.Callback()
					{
						@Override
						public void onConfirm(boolean withInstances)
						{

							for (AttributeType item : items)
							{
								removeType(item.getName(),
										withInstances);
							}
						}
					}).show();
		}
	}
	
	private class ExportActionHandler extends SingleActionHandler
	{
		public ExportActionHandler()
		{
			super(msg.getMessage("AttributeTypes.exportAction"), Images.save.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{			
			final Collection<AttributeType> items = getItems(target);
			SimpleFileDownloader downloader = new SimpleFileDownloader();
			addExtension(downloader);
			StreamResource resource = null;
			try
			{
				if (items.size() == 1)
				{
					AttributeType item = items.iterator().next();
					byte[] content = Constants.MAPPER.writeValueAsBytes(item);
					resource = new StreamResource(() -> {
						return new ByteArrayInputStream(content);
					}, item.getName() + ".json");
				} else
				{

					byte[] content = Constants.MAPPER.writeValueAsBytes(items);
					resource = new StreamResource(() -> {
						return new ByteArrayInputStream(content);
					}, "attributeTypes.json");
				}
			} catch (Exception e)
			{
				NotificationPopup.showError(msg,
						msg.getMessage("AttributeTypes.errorExport"), e);
				return;
			}
			
			
			downloader.setFileDownloadResource(resource);
			downloader.download();	
		}
	}
}
