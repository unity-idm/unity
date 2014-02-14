/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.messagetemplates;

import java.util.Map;

import pl.edu.icm.unity.notifications.MessageTemplate;
import pl.edu.icm.unity.notifications.MessageTemplate.Message;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * @author P. Piernik
 * 
 */
public class MessageTemplateViewer extends CustomComponent
{
	private UnityMessageSource msg;
	private Label name;
	private Label description;
	private Label consumer;
	private FormLayout messages;
	VerticalLayout main;
	

	public MessageTemplateViewer(UnityMessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		main = new VerticalLayout();
		FormLayout formLayout = new FormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("MessageTemplateViewer.name") + ":");
		description = new Label();
		description.setCaption(msg.getMessage("MessageTemplateViewer.description") + ":");
		consumer = new Label();
		consumer.setCaption(msg.getMessage("MessageTemplateViewer.consumer") + ":");
		formLayout.addComponents(name, description, consumer);
		messages = new FormLayout();		
		main.addComponent(formLayout);
		main.addComponent(messages);
		main.setSizeFull();
		setCompositionRoot(main);		
	}

	public void setInput(MessageTemplate template)
	{
		setEmpty();
		if (template == null)
		{
			main.setVisible(false);
			return;
		}
		main.setVisible(true);
		name.setValue(template.getName());
		description.setValue(template.getDescription());
		consumer.setValue(template.getConsumer());
		for (Map.Entry<String, Message> entry : template.getAllMessages().entrySet())
		{
			
			Label subject = new Label();
			subject .setCaption(msg.getMessage("MessageTemplateViewer.subject")  + ":");
			subject .setValue(entry.getValue().getSubject());
			Label body = new Label();
			body.setCaption(msg.getMessage("MessageTemplateViewer.body")  + ":");
			body.setValue(entry.getValue().getBody());
			
			String lcle = entry.getKey().toString();
			if (!lcle.equals(" "))
			{
				Label locale = new Label();
				locale.setCaption(msg.getMessage("MessageTemplateViewer.locale") + ":");
				locale.setValue(lcle);
				messages.addComponent(locale);
			}
			messages.addComponents(subject, body);

		}

	}

	private void setEmpty()
	{
		name.setValue("");
		description.setValue("");
		consumer.setValue("");
		messages.removeAllComponents();
	}

}
