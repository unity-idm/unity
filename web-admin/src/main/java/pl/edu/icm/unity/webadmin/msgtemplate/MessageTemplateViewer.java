/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.types.basic.MessageTemplate;

/**
 * Component presenting a complete information about message template.
 * @author P. Piernik
 * 
 */
public class MessageTemplateViewer extends MessageTemplateViewerBase
{	
	private Label description;
	private Label consumer;
	private Label notificationChannel;
	private Label messageType;
	
	private MessageTemplateConsumersRegistry registry;

	public MessageTemplateViewer(UnityMessageSource msg, MessageTemplateConsumersRegistry registry)
	{
		super(msg);
		this.registry = registry;
	}

	@Override
	protected void initUI()
	{	
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

	public void setTemplateInput(MessageTemplate template)
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
