/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.i18n.I18nLabelWithPreview;

/**
 * Component presenting a complete information about message template.
 * @author P. Piernik
 */
class MessageTemplateViewer extends VerticalLayout
{
	private MessageSource msg;
	private FormLayout main;
	private Label name;
	private Label description;
	private Label consumer;
	private Label notificationChannel;
	private Label messageType;
	
	private MessageTemplateConsumersRegistry registry;

	MessageTemplateViewer(MessageSource msg, MessageTemplateConsumersRegistry registry)
	{
		this.msg = msg;
		this.registry = registry;
		initUI();
	}

	private void initUI()
	{
		main = new CompactFormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("MessageTemplateViewer.name"));
		main.addComponent(name);
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);
		addComponents(main);
		setSizeFull();
		
		main.setMargin(true);
		main.setSpacing(true);
		description = new Label();
		description.setCaption(msg.getMessage("MessageTemplateViewer.description"));
		consumer = new Label();
		consumer.setCaption(msg.getMessage("MessageTemplateViewer.consumer"));
		notificationChannel = new Label();
		notificationChannel.setCaption(msg.getMessage("MessageTemplateViewer.notificationChannel"));	
		messageType = new Label();
		messageType.setCaption(msg.getMessage("MessageTemplateViewer.messageType"));
		main.addComponent(messageType, 1);
		main.addComponent(consumer, 1);	
		main.addComponent(notificationChannel, 1);	
		main.addComponent(description, 1);
		
	}

	public void setInput(MessageTemplate template)
	{   		
		String nameContent = template.getName(); 
		I18nString subjectContent = template.getMessage().getSubject();
		I18nString bodyContent = template.getMessage().getBody();
		
		main.setVisible(true);
		main.setSpacing(true);
		name.setValue(nameContent);
		
		if (!subjectContent.isEmpty())
		{
			I18nLabelWithPreview subject = I18nLabelWithPreview.builder(msg, 
					msg.getMessage("MessageTemplateViewer.subject"))
				.buildWithValue(subjectContent);
			main.addComponents(subject);
		}
		
		if (!bodyContent.isEmpty())
		{
			I18nLabelWithPreview body = I18nLabelWithPreview.builder(msg, 
					msg.getMessage("MessageTemplateViewer.body"))
				.withMode(template.getType() == MessageType.HTML ? 
						ContentMode.HTML : ContentMode.PREFORMATTED)
				.buildWithValue(bodyContent);
			main.addComponents(body);
		}
	}

	void clearContent()
	{
		removeAllComponents();
		initUI();
	}
	
	void setTemplateInput(MessageTemplate template)
	{   
		clearContent();
		description.setValue("");
		consumer.setValue("");	
		notificationChannel.setValue("");
		notificationChannel.setVisible(true);
		if (template == null)
		{	
			main.setVisible(false);	
			return;
		}
		setInput(template);	
		description.setValue(template.getDescription());
		messageType.setValue(template.getType().toString());
		String channel = template.getNotificationChannel();
		if (channel != null && !channel.isEmpty())
			notificationChannel.setValue(channel);
		else
			notificationChannel.setVisible(false);
		
		String cons = template.getConsumer();
		if (cons != null)
		{
			try
			{
				MessageTemplateDefinition cn = registry.getByName(cons);
				consumer.setValue(msg.getMessage(cn.getDescriptionKey()));
			} catch (IllegalArgumentException e)
			{
				consumer.setValue(template.getConsumer());
			}		
		}
	}
}
