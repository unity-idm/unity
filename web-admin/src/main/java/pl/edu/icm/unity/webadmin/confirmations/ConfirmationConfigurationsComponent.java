/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.confirmations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.msgtemplates.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

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
	private IdentityTypeSupport idMan;
	private NotificationsManagement notificationsMan;
	private AttributeTypeSupport atSupport;
	private GenericElementsTable<ConfirmationConfiguration> table;
	private com.vaadin.ui.Component main;
	private RadioButtonGroup<String> toConfirmType;
	private ConfirmationConfigurationViewer viewer;

	@Autowired
	public ConfirmationConfigurationsComponent(UnityMessageSource msg,
			ConfirmationConfigurationManagement configMan,
			MessageTemplateManagement msgMan, IdentityTypeSupport idMan,
			NotificationsManagement notificationsMan, AttributeTypeSupport attrsMan)
	{
		this.msg = msg;
		this.configMan = configMan;
		this.msgMan = msgMan;
		this.idMan = idMan;
		this.notificationsMan = notificationsMan;
		this.atSupport = attrsMan;
		
		setMargin(false);
		setHeight(100, Unit.PERCENTAGE);
		addStyleName(Styles.visibleScroll.toString());
		HorizontalLayout hl = new HorizontalLayout();
		setCaption(msg.getMessage("ConfirmationConfigurationsComponent.capion"));

		table = new GenericElementsTable<>(msg.getMessage(
				"ConfirmationConfigurationsComponent.configurationTable"),
				element -> element.getNameToConfirm());

		table.setWidth(90, Unit.PERCENTAGE);
		viewer = new ConfirmationConfigurationViewer(msg);
		viewer.setConfigurationInput(null);

		table.addSelectionListener(event -> {
			Collection<ConfirmationConfiguration> items = event.getAllSelectedItems();
			if (items.size() > 1 || items.isEmpty())
			{
				viewer.setConfigurationInput(null);
				return;
			}
			ConfirmationConfiguration item = items.iterator().next();
			viewer.setConfigurationInput(item);
		});

		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getDeleteAction());

		Toolbar<ConfirmationConfiguration> toolbar = new Toolbar<>(
				Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(100, Unit.PERCENTAGE);

		toConfirmType = new RadioButtonGroup<String>();
		toConfirmType.setItems(ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE);

		Map<String, String> captions = new HashMap<>();
		captions.put(ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE, msg
				.getMessage("ConfirmationConfigurationsComponent.configurationsForidentities"));
		captions.put(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE, msg
				.getMessage("ConfirmationConfigurationsComponent.configurationsForattributes"));
		toConfirmType.setItemCaptionGenerator(p -> captions.get(p));
		toConfirmType.setValue(ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE);
		toConfirmType.addValueChangeListener(e -> refresh());

		VerticalLayout left = new VerticalLayout();
		left.setMargin(false);
		left.addComponents(toConfirmType, tableWithToolbar);
		hl.addComponents(left, viewer);
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

	private List<String> getVerifiableAttrTypes()
	{
		List<String> vtypes = new ArrayList<String>();
		try
		{
			Collection<AttributeType> allTypes = atSupport.getAttributeTypes();
			for (AttributeType t : allTypes)
			{
				if (atSupport.getSyntax(t).isVerifiable())
					vtypes.add(t.getName());
			}
			if (vtypes.size() == 0)
				NotificationPopup.showNotice(msg, "", msg.getMessage(
						"ConfirmationConfigurationsComponent.firstAddAttribute"));
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage(
					"ConfirmationConfigurationsComponent.errorGetAttributeTypes"),
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
				if (idMan.getTypeDefinition(t.getIdentityTypeProvider())
						.isVerifiable())
					vtypes.add(t.getIdentityTypeProvider());
			}
			if (vtypes.isEmpty())
				NotificationPopup.showNotice(msg, "", msg.getMessage(
						"ConfirmationConfigurationsComponent.firstAddIdentity"));
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage(
					"ConfirmationConfigurationsComponent.errorGetIdentitiesTypes"),
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
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage(
					"ConfirmationConfigurationsComponent.errorGetConfigurations"),
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
			NotificationPopup.showError(msg, msg.getMessage(
					"ConfirmationConfigurationsComponent.errorAdd"), e);
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
			NotificationPopup.showError(msg, msg.getMessage(
					"ConfirmationConfigurationsComponent.errorUpdate"), e);
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
			NotificationPopup.showError(msg, msg.getMessage(
					"ConfirmationConfigurationsComponent.errorRemove"), e);
			return false;
		}
	}

	private boolean checkAvailableMsqTemplate()
	{
		try
		{
			Map<String, MessageTemplate> compatibleTemplates = msgMan
					.getCompatibleTemplates(EmailConfirmationTemplateDef.NAME);
			if (compatibleTemplates.isEmpty())
			{
				NotificationPopup.showNotice(msg, "", msg.getMessage(
						"ConfirmationConfigurationsComponent.firstAddMsqTemplate",
						EmailConfirmationTemplateDef.NAME));
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
				NotificationPopup.showNotice(msg, "", msg.getMessage(
						"ConfirmationConfigurationsComponent.firstAddNotificationChannel"));
				return false;
			}
			return true;

		} catch (Exception e)
		{
			return false;
		}
	}

	private SingleActionHandler<ConfirmationConfiguration> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, ConfirmationConfiguration.class)
				.withHandler(this::showAddDialog).build();
	}

	private void showAddDialog(Set<ConfirmationConfiguration> target)
	{
		if (!(checkAvailableMsqTemplate() && checkAvailableChannels()))
			return;
		ConfirmationConfigurationEditor editor = null;
		List<String> names = null;
		boolean attrConfig = toConfirmType.getValue()
				.equals(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE);
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
					NotificationPopup.showNotice(msg, "", msg.getMessage(
							"ConfirmationConfigurationsComponent.attributesConfigured"));

				} else
				{
					NotificationPopup.showNotice(msg, "", msg.getMessage(
							"ConfirmationConfigurationsComponent.identitiesConfigured"));

				}
				return;
			}

			editor = new ConfirmationConfigurationEditor(msg, notificationsMan, msgMan,
					toConfirmType.getValue().toString(), names, null);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"ConfirmationConfigurationsComponent.errorInFormEdit"), e);
			return;
		}

		ConfirmationConfigurationEditDialog dialog = new ConfirmationConfigurationEditDialog(
				msg,
				msg.getMessage("ConfirmationConfigurationsComponent.addAction"),
				confirmationConfig -> addConfiguration(confirmationConfig), editor);
		dialog.show();
	}

	private SingleActionHandler<ConfirmationConfiguration> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, ConfirmationConfiguration.class)
				.withHandler(selection -> refresh()).build();
	}

	private SingleActionHandler<ConfirmationConfiguration> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, ConfirmationConfiguration.class)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Set<ConfirmationConfiguration> items)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);
		new ConfirmDialog(msg,
				msg.getMessage("ConfirmationConfigurationsComponent.confirmDelete",
						confirmText),
				() -> {

					for (ConfirmationConfiguration item : items)
					{
						removeConfiguration(item.getTypeToConfirm(),
								item.getNameToConfirm());
					}

				}).show();
	}

	private SingleActionHandler<ConfirmationConfiguration> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, ConfirmationConfiguration.class)
				.withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<ConfirmationConfiguration> target)
	{
		ConfirmationConfiguration confirmationConfig = target.iterator().next();
		confirmationConfig = confirmationConfig.clone();
		ConfirmationConfigurationEditor editor;
		try
		{
			editor = new ConfirmationConfigurationEditor(msg, notificationsMan, msgMan,
					toConfirmType.getValue().toString(), null,
					confirmationConfig);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"ConfirmationConfigurationsComponent.errorInFormEdit"), e);
			return;
		}

		ConfirmationConfigurationEditDialog dialog = new ConfirmationConfigurationEditDialog(
				msg, msg.getMessage("MessageTemplatesComponent.editAction"),
				edidedConfig -> updateConfiguration(edidedConfig), editor);
		dialog.show();

	}
}
