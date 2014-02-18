/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Map;

import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Label;

/**
 * Simple component allowing to view all information about message template.
 * @author P. Piernik
 * 
 */
public class MessageTemplateViewer extends SimpleMessageTemplateViewer
{	
	private Label description;
	private Label consumer;

	public MessageTemplateViewer(String caption, UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		super(caption, msg, msgTempMan);
		initUI();
	}

	protected void initUI()
	{	
		main.setSpacing(true);
		main.setMargin(true);
		description = new Label();
		description.setCaption(msg.getMessage("MessageTemplateViewer.description") + ":");
		consumer = new Label();
		consumer.setCaption(msg.getMessage("MessageTemplateViewer.consumer") + ":");
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
		String cons = template.getConsumer();
		if (cons != null)
		{
			consumer.setValue(template.getConsumer());
		}
		for (Map.Entry<String, Message> entry : template.getAllMessages().entrySet())
		{
			AbstractTextField subject = new DescriptionTextArea();
			subject.setCaption(msg.getMessage("MessageTemplateViewer.subject")  + ":");
			subject.setValue(entry.getValue().getSubject());
			subject.setReadOnly(true);
			AbstractTextField body = new DescriptionTextArea();
			body.setCaption(msg.getMessage("MessageTemplateViewer.body")  + ":");
			body.setValue(entry.getValue().getBody());
			body.setReadOnly(true);
			String lcle = entry.getKey().toString();
//		 	For future, full support to locale. 
			if (!lcle.equals(""))
			{
				Label locale = new Label();
				locale.setCaption(msg.getMessage("MessageTemplateViewer.locale") + ":");
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
