/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.messagetemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.notifications.MessageTemplate;
import pl.edu.icm.unity.notifications.MessageTemplate.Message;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Component;
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
	private List<Component> messages;
	private FormLayout main;
	

	public MessageTemplateViewer(UnityMessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		messages = new ArrayList<Component>();
		main = new FormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("MessageTemplateViewer.name") + ":");
		description = new Label();
		description.setCaption(msg.getMessage("MessageTemplateViewer.description") + ":");
		consumer = new Label();
		consumer.setCaption(msg.getMessage("MessageTemplateViewer.consumer") + ":");
		main.addComponents(name, description, consumer);		
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
			
			AbstractTextField subject = new DescriptionTextArea();
			subject.setCaption(msg.getMessage("MessageTemplateViewer.subject")  + ":");
			subject.setValue(entry.getValue().getSubject());
			subject.setReadOnly(true);
			subject.setId("subject");
			AbstractTextField body = new DescriptionTextArea();
			body.setCaption(msg.getMessage("MessageTemplateViewer.body")  + ":");
			body.setValue(entry.getValue().getBody());
			body.setReadOnly(true);
			body.setId("body");
			String lcle = entry.getKey().toString();
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

	private void setEmpty()
	{
		name.setValue("");
		description.setValue("");
		consumer.setValue("");
		for (Component c : messages)
		{
			main.removeComponent(c);
		}
		messages.clear();
	}

}
