/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.confirmation;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.RequiredComboBox;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;

/**
 * Component to edit or add confirmation configuration
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationConfigurationEditor extends FormLayout
{

	private UnityMessageSource msg;

	private NotificationsManagement notificationsMan;

	private MessageTemplateManagement msgMan;

	private ComboBox type;

	private ComboBox msgTemplate;

	private ComboBox notificationChannel;

	private String forType;

	protected ConfirmationConfigurationEditor(UnityMessageSource msg,
			NotificationsManagement notificationsMan, AttributesManagement attrsMan,
			MessageTemplateManagement msgMan, String forType, List<String> names,
			ConfirmationConfiguration toEdit) throws EngineException
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

		type = new RequiredComboBox(msg);
		if (forType.equals(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE))
			type.setCaption(msg
					.getMessage("ConfirmationConfigurationViewer.forAttributeType"));
		else
			type.setCaption(msg
					.getMessage("ConfirmationConfigurationViewer.forIdentityType"));

		type.setImmediate(true);
		type.setValidationVisible(false);
		type.setNullSelectionAllowed(false);

		if (names != null)
			for (String n : names)
			{
				type.addItem(n);
			}
		if (type.size() > 0)
		{
			type.setValue(type.getItemIds().toArray()[0]);
		}

		notificationChannel = new RequiredComboBox(
				msg.getMessage("ConfirmationConfigurationViewer.notificationChannel"),
				msg);
		notificationChannel.setImmediate(true);
		notificationChannel.setValidationVisible(false);
		notificationChannel.setNullSelectionAllowed(false);
		Set<String> channels = notificationsMan.getNotificationChannels().keySet();
		for (String c : channels)
			notificationChannel.addItem(c);
		if (notificationChannel.size() > 0)
		{
			notificationChannel.setValue(notificationChannel.getItemIds().toArray()[0]);
		}

		msgTemplate = new CompatibleTemplatesComboBox(ConfirmationTemplateDef.NAME, msgMan);
		msgTemplate.setCaption(msg
				.getMessage("ConfirmationConfigurationViewer.msgTemplate"));
		msgTemplate.setValidationVisible(false);
		msgTemplate.setNullSelectionAllowed(false);
		msgTemplate.setRequired(true);
		msgTemplate.setRequiredError(msg.getMessage("fieldRequired"));
		msgTemplate.setImmediate(true);

		if (msgTemplate.size() > 0)
		{
			msgTemplate.setValue(msgTemplate.getItemIds().toArray()[0]);
		}

		if (editMode)
		{
			type.addItem(toEdit.getNameToConfirm());
			type.setValue(toEdit.getNameToConfirm());
			type.setReadOnly(true);
			msgTemplate.setValue(toEdit.getMsgTemplate());
			notificationChannel.setValue(toEdit.getNotificationChannel());
		}

		addComponents(type, msgTemplate, notificationChannel);
		setSizeFull();
	}

	private boolean validate()
	{
		return type.isValid() && msgTemplate.isValid() && notificationChannel.isValid();
	}
	
	public ConfirmationConfiguration getConfirmationConfiguration()
	{
		if (!validate())
			return null;
		return new ConfirmationConfiguration(forType, type.getValue().toString(),
				notificationChannel.getValue().toString(), msgTemplate.getValue()
						.toString());
	}
}
