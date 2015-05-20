/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.confirmations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
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
	private IdentitiesManagement idMan;
	private NotificationsManagement notificationsMan;
	private AttributesManagement attrsMan;
	private GenericElementsTable<ConfirmationConfiguration> table;
	private com.vaadin.ui.Component main;
	private OptionGroup toConfirmType;
	private ConfirmationConfigurationViewer viewer;

	@Autowired
	public ConfirmationConfigurationsComponent(UnityMessageSource msg,
			ConfirmationConfigurationManagement configMan,
			MessageTemplateManagement msgMan, IdentitiesManagement idMan,
			NotificationsManagement notificationsMan, AttributesManagement attrsMan)
	{
		this.msg = msg;
		this.configMan = configMan;
		this.msgMan = msgMan;
		this.idMan = idMan;
		this.notificationsMan = notificationsMan;
		this.attrsMan = attrsMan;

		addStyleName(Styles.visibleScroll.toString());
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
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());

		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);

		toConfirmType = new OptionGroup();
		toConfirmType.setImmediate(true);
		toConfirmType.addItem(ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE);
		toConfirmType.setItemCaption(
				ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
				msg.getMessage("ConfirmationConfigurationsComponent.configurationsForidentities"));
		toConfirmType.addItem(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE);
		toConfirmType.setItemCaption(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				msg.getMessage("ConfirmationConfigurationsComponent.configurationsForattributes"));
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
		WebSession.getCurrent().getEventBus()
				.addListener(new EventListener<AttributeTypesUpdatedEvent>()
				{
					@Override
					public void handleEvent(AttributeTypesUpdatedEvent event)
					{
						refresh();
					}
				}, AttributeTypesUpdatedEvent.class);
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

	private List<String> getVerifiableAttrTypes()
	{
		List<String> vtypes = new ArrayList<String>();
		try
		{
			Collection<AttributeType> allTypes = attrsMan.getAttributeTypes();
			for (AttributeType t : allTypes)
			{
				if (t.getValueType().isVerifiable())
					vtypes.add(t.getName());
			}
			if (vtypes.size() == 0)
				NotificationPopup.showNotice(
						msg,
						"",
						msg.getMessage("ConfirmationConfigurationsComponent.firstAddAttribute"));
		} catch (EngineException e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg
					.getMessage("ConfirmationConfigurationsComponent.errorGetAttributeTypes"),
					e);
		}
		return vtypes;
	}

	private List<String> getVerifiableIdTypes()
	{
		List<String> vtypes = new ArrayList<String>();
		try
		{
			Collection<IdentityType> ids = idMan.getIdentityTypes();
			for (IdentityType t : ids)
			{
				if (t.getIdentityTypeProvider().isVerifiable())
					vtypes.add(t.getIdentityTypeProvider().getId());
			}
			if (vtypes.isEmpty())
				NotificationPopup.showNotice(
						msg,
						"",
						msg.getMessage("ConfirmationConfigurationsComponent.firstAddIdentity"));

		} catch (EngineException e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg
					.getMessage("ConfirmationConfigurationsComponent.errorGetIdentitiesTypes"),
					e);
		}
		return vtypes;
	}

	private List<ConfirmationConfiguration> getConfigurations() throws EngineException
	{
		List<ConfirmationConfiguration> allCfgs = configMan.getAllConfigurations();
		List<ConfirmationConfiguration> viewedCfgs = new ArrayList<ConfirmationConfiguration>();
		viewedCfgs.addAll(allCfgs);
		for (ConfirmationConfiguration c : allCfgs)
			if (!c.getTypeToConfirm().equals(toConfirmType.getValue()))
				viewedCfgs.remove(c);
		return viewedCfgs;
	}

	private void refresh()
	{
		try
		{
			table.setInput(getConfigurations());
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

	private boolean addConfiguration(ConfirmationConfiguration cfg)
	{
		try
		{
			configMan.addConfiguration(cfg);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg,
					msg.getMessage("ConfirmationConfigurationsComponent.errorAdd"),
					e);
			return false;
		}
	}

	private boolean updateConfiguration(ConfirmationConfiguration cfg)
	{
		try
		{
			configMan.updateConfiguration(cfg);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg,
					msg.getMessage("ConfirmationConfigurationsComponent.errorUpdate"),
					e);
			return false;
		}
	}

	private boolean removeConfiguration(String type, String name)
	{
		try
		{
			configMan.removeConfiguration(type, name);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg,
					msg.getMessage("ConfirmationConfigurationsComponent.errorRemove"),
					e);
			return false;
		}
	}

	private boolean checkAvailableMsqTemplate()
	{
		try
		{
			Map<String, MessageTemplate> compatibleTemplates = msgMan
					.getCompatibleTemplates(ConfirmationTemplateDef.NAME);
			if (compatibleTemplates.isEmpty())
			{
				NotificationPopup.showNotice(
						msg,
						"",
						msg.getMessage("ConfirmationConfigurationsComponent.firstAddMsqTemplate",
								ConfirmationTemplateDef.NAME));
				return false;
			}
			return true;

		} catch (Exception e)
		{
			return false;
		}
	}
	
	private boolean checkAvailableChannels()
	{
		try
		{
			if (notificationsMan.getNotificationChannels().isEmpty())
			{
				NotificationPopup.showNotice(
						msg,
						"",
						msg.getMessage("ConfirmationConfigurationsComponent.firstAddNotificationChannel"));
				return false;
			}
			return true;

		} catch (Exception e)
		{
			return false;
		}
	}

	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("ConfirmationConfigurationsComponent.addAction"),
					Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{	
			if (!(checkAvailableMsqTemplate() && checkAvailableChannels()))
				return;
			ConfirmationConfigurationEditor editor = null;
			List<String> names = null;
			boolean attrConfig = toConfirmType.getValue().equals(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE);
			if (attrConfig)
			{
				names = getVerifiableAttrTypes();
			} else
			{
				names = getVerifiableIdTypes();
			}

			if (names.isEmpty())
			   return;

			try
			{
				for (ConfirmationConfiguration c : getConfigurations())
				{
					if (names.contains(c.getNameToConfirm()))
						names.remove(c.getNameToConfirm());
				}
				if (names.isEmpty())
				{
					if (attrConfig)
					{
						NotificationPopup.showNotice(
								msg,
								"",
								msg.getMessage("ConfirmationConfigurationsComponent.attributesConfigured"));

					} else
					{
						NotificationPopup.showNotice(
								msg,
								"",
								msg.getMessage("ConfirmationConfigurationsComponent.identitiesConfigured"));

					}
					return;
				}

				editor = new ConfirmationConfigurationEditor(msg, notificationsMan,
						 msgMan, toConfirmType.getValue()
								.toString(), names, null);
			} catch (EngineException e)
			{
				NotificationPopup.showError(
						msg,
						msg.getMessage("ConfirmationConfigurationsComponent.errorInFormEdit"),
						e);
				return;
			}
			ConfirmationConfigurationEditDialog dialog = new ConfirmationConfigurationEditDialog(
					msg,
					msg.getMessage("ConfirmationConfigurationsComponent.addAction"),
					new ConfirmationConfigurationEditDialog.Callback()
					{

						@Override
						public boolean newConfirmationConfiguration(
								ConfirmationConfiguration configuration)
						{
							return addConfiguration(configuration);
						}

					}, editor);
			dialog.show();
		}
	}

	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("ConfirmationConfigurationsComponent.refreshAction"),
					Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}

	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("ConfirmationConfigurationsComponent.deleteAction"),
					Images.delete.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<ConfirmationConfiguration> items = getItems(target);
			StringBuilder confirmText = new StringBuilder();
			Iterator<ConfirmationConfiguration> it = items.iterator();
			final int MAX = 4;
			for (int i = 0; i < MAX && it.hasNext(); i++)
				confirmText.append(", ").append(it.next().getNameToConfirm());
			if (it.hasNext())
				confirmText.append(msg.getMessage("MessageUtils.andMore",
						items.size() - MAX));

			new ConfirmDialog(msg, msg.getMessage(
					"ConfirmationConfigurationsComponent.confirmDelete",
					confirmText.substring(2)), new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (ConfirmationConfiguration item : items)
					{
						removeConfiguration(item.getTypeToConfirm(),
								item.getNameToConfirm());
					}
				}
			}).show();

		}
	}

	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("ConfirmationConfigurationsComponent.editAction"),
					Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{

			GenericItem<?> witem = (GenericItem<?>) target;
			ConfirmationConfiguration item = (ConfirmationConfiguration) witem
					.getElement();
			ConfirmationConfigurationEditor editor;
			try
			{
				editor = new ConfirmationConfigurationEditor(msg, notificationsMan,
						msgMan, toConfirmType.getValue().toString(), null,
						item);
			} catch (EngineException e)
			{
				NotificationPopup.showError(
						msg,
						msg.getMessage("ConfirmationConfigurationsComponent.errorInFormEdit"),
						e);
				return;
			}

			ConfirmationConfigurationEditDialog dialog = new ConfirmationConfigurationEditDialog(
					msg,
					msg.getMessage("MessageTemplatesComponent.editAction"),
					new ConfirmationConfigurationEditDialog.Callback()
					{
						@Override
						public boolean newConfirmationConfiguration(
								ConfirmationConfiguration configuration)
						{
							return updateConfiguration(configuration);
						}
					}, editor);
			dialog.show();
		}
	}

}
