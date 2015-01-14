/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.confirmation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.AttributeNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for confirmation configuration management
 * 
 * @author P. Piernik
 * 
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConfirmationConfigurationsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private ConfirmationConfigurationManagement configMan;
	private MessageTemplateManagement msgMan;
	private IdentityTypesRegistry identityTypesRegistry;
	private NotificationsManagement notificationsMan;
	private AttributesManagement attrsMan;

	private GenericElementsTable<ConfirmationConfiguration> table;
	private com.vaadin.ui.Component main;
	private OptionGroup toConfirmType;
	private ConfirmationConfigurationViewer viewer;

	@Autowired
	public ConfirmationConfigurationsComponent(UnityMessageSource msg,
			ConfirmationConfigurationManagement configMan,
			MessageTemplateManagement msgMan,
			IdentityTypesRegistry identityTypesRegistry,
			NotificationsManagement notificationsMan, AttributesManagement attrsMan)
	{
		this.msg = msg;
		this.configMan = configMan;
		this.msgMan = msgMan;
		this.identityTypesRegistry = identityTypesRegistry;
		this.notificationsMan = notificationsMan;
		this.attrsMan = attrsMan;

		HorizontalLayout hl = new HorizontalLayout();
		setCaption(msg.getMessage("ConfirmationConfigurationsComponent.capion"));
		table = new GenericElementsTable<ConfirmationConfiguration>(
				msg.getMessage("ConfirmationConfigurationsComponent.configurationTable"),
				ConfirmationConfiguration.class,
				new GenericElementsTable.NameProvider<ConfirmationConfiguration>()
				{
					@Override
					public Label toRepresentation(
							ConfirmationConfiguration element)
					{
						return new Label(element.getNameToConfirm());
					}
				});
		table.setMultiSelect(true);
		table.setWidth(90, Unit.PERCENTAGE);
		viewer = new ConfirmationConfigurationViewer(msg);
		viewer.setConfigurationInput(null);

		table.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<ConfirmationConfiguration> items = getItems(table
						.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setConfigurationInput(null);
					return;
				}
				ConfirmationConfiguration item = items.iterator().next();
				viewer.setConfigurationInput(item);
			}
		});
		// table.addActionHandler(new RefreshActionHandler());
		// table.addActionHandler(new EditActionHandler());

		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);

		toConfirmType = new OptionGroup();
		toConfirmType.setImmediate(true);
		toConfirmType.addItem(ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE);
		toConfirmType.setItemCaption(ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE, 
				msg.getMessage("ConfirmationConfigurationsComponent.identities"));
		toConfirmType.addItem(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE);
		toConfirmType.setItemCaption(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE, 
				msg.getMessage("ConfirmationConfigurationsComponent.attributes"));
		toConfirmType.setNullSelectionAllowed(false);
		toConfirmType.select(ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE);
		toConfirmType.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				refresh();
			}
		});
		
		VerticalLayout left = new VerticalLayout();
		left.setSpacing(true);
		left.addComponents(toConfirmType, tableWithToolbar);
				
		hl.addComponents(left, viewer);
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(left, 0.3f);
		hl.setExpandRatio(viewer, 0.7f);
		refresh();
	}

	private Collection<ConfirmationConfiguration> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<ConfirmationConfiguration> items = new ArrayList<ConfirmationConfiguration>();
		for (Object o : c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((ConfirmationConfiguration) i.getElement());
		}
		return items;
	}
	
	private List<AttributeType> getVerifiableAttrTypes()
	{
		List<AttributeType> vtypes = new ArrayList<AttributeType>();
		try
		{
			
			Collection<AttributeType>  allTypes = attrsMan.getAttributeTypes();
			for (AttributeType t : allTypes)
			{
				if (t.getValueType().hasValuesVerifiable())
					vtypes.add(t);
			}
		} catch (EngineException e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg
					.getMessage("ConfirmationConfigurationsComponent.errorGetAttributeTypes"),
					e);
		}
		return vtypes;
	}

	private void refresh()
	{
		try
		{
			Collection<ConfirmationConfiguration> templates = configMan
					.getAllConfigurations();
			table.setInput(templates);
			viewer.setConfigurationInput(null);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg
					.getMessage("ConfirmationConfigurationsComponent.errorGetConfigurations"),
					e);
			removeAllComponents();
			addComponent(error);
		}

	}

}
