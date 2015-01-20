/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;

import com.vaadin.ui.Label;

/**
 * Component presenting a complete information about message template.
 * FIXME - inheritance/OO is poorely implemented.
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
		main.setSpacing(true);
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

		I18nLabel subject = new I18nLabel(msg, msg.getMessage("MessageTemplateViewer.subject"));
		subject.setValue(template.getMessage().getSubject());
		I18nLabel body = new I18nLabel(msg, msg.getMessage("MessageTemplateViewer.body"));
		body.setValue(template.getMessage().getBody());
		messages.add(subject);
		messages.add(body);
		main.addComponents(subject, body);
	}

	@Override
	protected void setEmpty()
	{
		super.setEmpty();
		description.setValue("");
		consumer.setValue("");
	}
}
