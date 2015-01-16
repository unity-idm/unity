/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.confirmation;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Simple component allowing to view confirmation configuration.
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationConfigurationViewer extends VerticalLayout
{
	protected UnityMessageSource msg;
	private Label type;
	private Label template;
	private Label channel;
	private FormLayout main;

	public ConfirmationConfigurationViewer(UnityMessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		main = new FormLayout();
		type = new Label();
		template = new Label();
		template.setCaption(msg.getMessage("ConfirmationConfigurationViewer.msgTemplate"));

		channel = new Label();
		channel.setCaption(msg
				.getMessage("ConfirmationConfigurationViewer.notificationChannel"));
		main.addComponents(type, template, channel);
		main.setSizeFull();
		addComponent(main);
		setSizeFull();
	}

	public void setConfigurationInput(ConfirmationConfiguration cfg)
	{
		if (cfg == null)
		{
			type.setValue("");
			template.setValue("");
			channel.setValue("");
			main.setVisible(false);
			return;
		}
		boolean attrType = cfg.getTypeToConfirm().equals(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE);
		if (attrType)
			type.setCaption(msg
					.getMessage("ConfirmationConfigurationViewer.forAttributeType"));
		else
			type.setCaption(msg
					.getMessage("ConfirmationConfigurationViewer.forIdentityType"));

		main.setVisible(true);
		type.setValue(cfg.getNameToConfirm());
		template.setValue(cfg.getMsgTemplate());
		channel.setValue(cfg.getNotificationChannel());

	}

}
