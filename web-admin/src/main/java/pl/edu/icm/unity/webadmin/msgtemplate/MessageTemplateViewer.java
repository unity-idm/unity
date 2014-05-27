/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Map;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;

/**
 * Simple component allowing to view all information about message template.
 * @author P. Piernik
 * 
 */
public class MessageTemplateViewer extends SimpleMessageTemplateViewer
{	
	private DescriptionTextArea description;
	private Label consumer;
	private MessageTemplateConsumersRegistry registry;

	public MessageTemplateViewer(String caption, UnityMessageSource msg,
			MessageTemplateManagement msgTempMan, MessageTemplateConsumersRegistry registry)
	{
		super(caption, msg, msgTempMan);
		this.registry = registry;
		initUI();
	}

	protected void initUI()
	{	
		main.setSpacing(true);
		main.setMargin(true);
		description = new DescriptionTextArea();
		description.setCaption(msg.getMessage("MessageTemplateViewer.description"));
		description.setReadOnly(true);
		consumer = new Label();
		consumer.setCaption(msg.getMessage("MessageTemplateViewer.consumer"));
		consumer.setReadOnly(true);
		main.addComponent(consumer, 1);	
		main.addComponent(description, 1);
		
	}

	public void setTemplateInput(MessageTemplate template)
	{   
		notSet.setVisible(false);
		setEmpty();
		if (template == null)
		{
			main.setVisible(false);	
			return;
		}
		main.setVisible(true);
		name.setValue(template.getName());
		description.setValue(template.getDescription());
		description.setRows(template.getDescription().split("\n").length);
		String cons = template.getConsumer();
		if (cons != null)
		{
			try
			{
				MessageTemplateDefinition cn = registry.getByName(cons);
				consumer.setValue(msg.getMessage(cn.getDescriptionKey()));
			} catch (IllegalTypeException e)
			{
				consumer.setValue(template.getConsumer());
			}
			
		}
		for (Map.Entry<String, Message> entry : template.getAllMessages().entrySet())
		{
			Label subject = new Label(entry.getValue().getSubject());
			subject.setCaption(msg.getMessage("MessageTemplateViewer.subject"));
			TextArea body = new DescriptionTextArea();
			body.setCaption(msg.getMessage("MessageTemplateViewer.body"));
			body.setValue(entry.getValue().getBody());
			body.setReadOnly(true);
			body.setRows(entry.getValue().getBody().split("\n").length + 1);
			String lcle = entry.getKey().toString();
//		 	For future, full support to locale. 
			if (!lcle.equals(""))
			{
				Label locale = new Label();
				locale.setCaption(msg.getMessage("MessageTemplateViewer.locale"));
				locale.setValue(lcle);
				messages.add(locale);
				main.addComponent(locale);

			}
			messages.add(subject);
			messages.add(body);
			main.addComponents(subject, body);
		}
	}

	protected void setEmpty()
	{
		super.setEmpty();
		description.setValue("");
		consumer.setValue("");
		
	}

}
