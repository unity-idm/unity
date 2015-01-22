/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

import com.vaadin.ui.Label;

/**
 * Component presenting a complete information about message template.
 * @author P. Piernik
 * 
 */
public class MessageTemplateViewer extends MessageTemplateViewerBase
{	
	private DescriptionTextArea description;
	private Label consumer;
	private MessageTemplateConsumersRegistry registry;

	public MessageTemplateViewer(UnityMessageSource msg, MessageTemplateConsumersRegistry registry)
	{
		super(msg);
		this.registry = registry;
	}

	protected void initUI()
	{	
		main.setMargin(true);
		main.setSpacing(true);
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
		clearContent();
		description.setValue("");
		consumer.setValue("");	
		if (template == null)
		{	
			main.setVisible(false);	
			return;
		}
		setInput(template.getName(), template.getMessage().getSubject(), template.getMessage().getBody());	
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
	}
}
