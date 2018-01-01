/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.confirmations;

import java.util.List;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.msgtemplates.confirm.ConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox2;

/**
 * Component to edit or add confirmation configuration
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationConfigurationEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private NotificationsManagement notificationsMan;
	private MessageTemplateManagement msgMan;
	private ComboBox<String> type;
	private CompatibleTemplatesComboBox2 msgTemplate;
	private ComboBox<String> notificationChannel;
	private TextField validityTime;
	private String forType;
	private Binder<ConfirmationConfiguration> binder;

	public ConfirmationConfigurationEditor(UnityMessageSource msg,
			NotificationsManagement notificationsMan, MessageTemplateManagement msgMan,
			String forType, List<String> names, ConfirmationConfiguration toEdit)
			throws EngineException
	{
		this.msg = msg;
		this.notificationsMan = notificationsMan;
		this.msgMan = msgMan;
		this.forType = forType;
		initUI(toEdit, names);
	}

	private void initUI(ConfirmationConfiguration toEdit, List<String> names)
			throws EngineException
	{
		boolean editMode = toEdit != null;

		type = new ComboBox<String>();
		if (forType.equals(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE))
			type.setCaption(msg.getMessage(
					"ConfirmationConfigurationViewer.forAttributeType"));
		else
			type.setCaption(msg.getMessage(
					"ConfirmationConfigurationViewer.forIdentityType"));
		type.setEmptySelectionAllowed(false);
		if (names != null)
		{
			type.setItems(names);
			if (!names.isEmpty())
				type.setValue(names.iterator().next());
		}

		notificationChannel = new ComboBox<String>(msg
				.getMessage("ConfirmationConfigurationViewer.notificationChannel"));
		notificationChannel.setEmptySelectionAllowed(false);
		Set<String> channels = notificationsMan.getNotificationChannels().keySet();
		if (channels != null)
		{
			notificationChannel.setItems(channels);
			if (!channels.isEmpty())
				notificationChannel.setValue(channels.iterator().next());
		}

		msgTemplate = new CompatibleTemplatesComboBox2(ConfirmationTemplateDef.NAME,
				msgMan);
		msgTemplate.setCaption(
				msg.getMessage("ConfirmationConfigurationViewer.msgTemplate"));
		msgTemplate.setEmptySelectionAllowed(false);
		msgTemplate.setDefaultValue();

		validityTime = new TextField(msg.getMessage("ConfirmationConfigurationViewer.validityTime"));
		
		addComponents(type, msgTemplate, notificationChannel, validityTime);

		binder = new Binder<>(ConfirmationConfiguration.class);
		binder.forField(type).asRequired(msg.getMessage("fieldRequired"))
				.bind("nameToConfirm");
		binder.forField(notificationChannel).asRequired(msg.getMessage("fieldRequired"))
				.bind("notificationChannel");
		binder.forField(msgTemplate).asRequired(msg.getMessage("fieldRequired"))
				.bind("msgTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
			.withConverter(new StringToIntegerConverter(msg.getMessage("notAnIntNumber")))
			.withValidator(new IntegerRangeValidator(
					msg.getMessage("outOfBoundsNumber", 1, 60*24*365), 1, 60*24*365))
			.bind("validityTime");

		ConfirmationConfiguration init = editMode ? toEdit
				: new ConfirmationConfiguration(forType, type.getValue(),
						notificationChannel.getValue(),
						msgTemplate.getValue(),
						48*60);
		binder.setBean(init);
	}

	public ConfirmationConfiguration getConfirmationConfiguration()
	{
		if (!binder.isValid())
		{
			binder.validate();
			return null;
		}
		ConfirmationConfiguration conCfg = binder.getBean();
		conCfg.setTypeToConfirm(forType);
		return conCfg;
	}
}
