/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
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
@PrototypeComponent
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
	
	
	@Autowired
	public AttributeTypesComponent(UnityMessageSource msg, AttributeTypeManagement attrManagement,
			AttributeTypeSupport atSupport, 
			AttributeHandlerRegistry attrHandlerRegistry, 
			AttributeMetadataHandlerRegistry attrMetaHandlerRegistry,
			UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.attrManagement = attrManagement;
		this.atSupport = atSupport;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.attrMetaHandlerRegistry = attrMetaHandlerRegistry;
		this.bus = WebSession.getCurrent().getEventBus();
		this.serverConfig = serverConfig;
		
		setMargin(false);
		HorizontalLayout hl = new HorizontalLayout();
		
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("AttributeTypes.caption"));
		table = new GenericElementsTable<>(msg.getMessage("AttributeTypes.types"), 
				element -> element.getName());
		table.setStyleGenerator(at ->  at.isTypeImmutable() ? 
				Styles.immutableAttribute.toString() : "");
		viewer = new AttributeTypeViewer(msg);
		table.setMultiSelect(true);
		table.addSelectionListener(event ->
		{
			Collection<AttributeType> items = event.getAllSelectedItems();
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
		});
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getCopyAction());
		table.addActionHandler(getDeleteAction());
		table.addActionHandler(getExportAction());
		table.addActionHandler(getImportAction());
		
		Toolbar<AttributeType> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(100, Unit.PERCENTAGE);

		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
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
	
	private SingleActionHandler<AttributeType> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, AttributeType.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler<AttributeType> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, AttributeType.class)
				.withHandler(this::showAddDialog)
				.build();
	}
	
	private void showAddDialog(Set<AttributeType> target)
	{
		RegularAttributeTypeEditor editor = new RegularAttributeTypeEditor(msg, attrHandlerRegistry, 
				attrMetaHandlerRegistry, atSupport);
		AttributeTypeEditDialog dialog = new AttributeTypeEditDialog(msg, 
				msg.getMessage("AttributeTypes.addAction"), 
				newAttributeType -> addType(newAttributeType), editor);
		dialog.show();
	}
	
	private SingleActionHandler<AttributeType> getImportAction()
	{
		return SingleActionHandler.builder(AttributeType.class)
				.withCaption(msg.getMessage("AttributeTypes.importAction"))
				.withIcon(Images.download.getResource())
				.withHandler(this::importHandler)
				.withDisabledPredicate(at -> at.isTypeImmutable())
				.dontRequireTarget()
				.build();
	}
	
	private void importHandler(Set<AttributeType> items)
	{
		ImportAttributeTypeDialog dialog = new ImportAttributeTypeDialog(msg,
				msg.getMessage("AttributeTypes.importAction"), serverConfig,
				attrManagement, atSupport, () -> refresh());
		dialog.show();
	}
		
	private SingleActionHandler<AttributeType> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, AttributeType.class)
				.withHandler(this::showEditDialog)
				.withDisabledPredicate(at -> (at.isTypeImmutable() && at.isInstanceImmutable()))
				.build();
	}

	
	private void showEditDialog(Collection<AttributeType> target)
	{
		AttributeType at = target.iterator().next();
		at = at.clone();
		AttributeTypeEditor editor = at.isTypeImmutable() ? 
				new ImmutableAttributeTypeEditor(msg, at) : 
				new RegularAttributeTypeEditor(msg, attrHandlerRegistry, at, 
						attrMetaHandlerRegistry, atSupport);
				AttributeTypeEditDialog dialog = new AttributeTypeEditDialog(msg, 
						msg.getMessage("AttributeTypes.editAction"), 
						newAttributeType -> updateType(newAttributeType), 
						editor);
				dialog.show();
	}

	private SingleActionHandler<AttributeType> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, AttributeType.class)
				.withHandler(this::deleteHandler)
				.withDisabledPredicate(at -> at.isTypeImmutable())
				.build();
	}
	
	private void deleteHandler(Set<AttributeType> items)
	{	
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);
		new ConfirmWithOptionDialog(msg, msg.getMessage(
				"AttributeTypes.confirmDelete", confirmText),
				msg.getMessage("AttributeTypes.withInstances"),
				withInstances ->
				{
					for (AttributeType item : items)
						removeType(item.getName(), withInstances);
				}).show();
	}
	
	private SingleActionHandler<AttributeType> getCopyAction()
	{
		return SingleActionHandler.builder(AttributeType.class)
				.withCaption(msg.getMessage("AttributeTypes.copyAction"))
				.withIcon(Images.copy.getResource())
				.withHandler(this::copyHandler)
				.withDisabledPredicate(at -> at.isTypeImmutable())
				.build();
	}
	
	private void copyHandler(Set<AttributeType> items)
	{
		AttributeType at = items.iterator().next();
		at = at.clone();
		RegularAttributeTypeEditor editor = 
				new RegularAttributeTypeEditor(msg, attrHandlerRegistry, at, 
						attrMetaHandlerRegistry, atSupport);
		editor.setCopyMode();

		AttributeTypeEditDialog dialog = new AttributeTypeEditDialog(msg, 
				msg.getMessage("AttributeTypes.copyAction"), 
				newAttributeType -> addType(newAttributeType), 
				editor);
		dialog.show();
	}

	private SingleActionHandler<AttributeType> getExportAction()
	{
		return SingleActionHandler.builder(AttributeType.class)
				.withCaption(msg.getMessage("AttributeTypes.exportAction"))
				.withIcon(Images.export.getResource())
				.multiTarget()
				.withHandler(this::exportHandler)
				.withDisabledPredicate(at -> at.isTypeImmutable())
				.build();
	}
	
	private void exportHandler(Set<AttributeType> items)
	{			
		SimpleFileDownloader downloader = new SimpleFileDownloader();
		addExtension(downloader);
		StreamResource resource = null;
		try
		{
			if (items.size() == 1)
			{
				AttributeType item = items.iterator().next();
				byte[] content = Constants.MAPPER.writeValueAsBytes(item);
				resource = new StreamResource(
						() -> new ByteArrayInputStream(content),
						item.getName() + ".json");
			} else
			{

				byte[] content = Constants.MAPPER.writeValueAsBytes(items);
				resource = new StreamResource(
						() -> new ByteArrayInputStream(content),
						"attributeTypes.json");
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
