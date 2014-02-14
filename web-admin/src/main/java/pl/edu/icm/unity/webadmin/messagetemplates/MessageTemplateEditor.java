/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.messagetemplates;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.notifications.MessageTemplate;
import pl.edu.icm.unity.notifications.MessageTemplate.Message;
import pl.edu.icm.unity.notifications.MessageTemplateConsumer;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

public class MessageTemplateEditor extends VerticalLayout
{
	private UnityMessageSource msg;
	private MessageTemplateConsumersRegistry registry;
	private AbstractTextField name;
	private DescriptionTextArea description;
	private AbstractTextField subject;
	private DescriptionTextArea body;
	private ComboBox consumer;
	private Label consumerDescription;
	private boolean editMode;
	private HorizontalLayout buttonsSub;
	private HorizontalLayout buttonsBody;

	public MessageTemplateEditor(UnityMessageSource msg,
			MessageTemplateConsumersRegistry registry, MessageTemplate toEdit)
	{
		super();
		editMode = toEdit != null;
		this.msg = msg;
		this.registry = registry;
		initUI(toEdit);

	}

	private void initUI(MessageTemplate toEdit)
	{
		
		buttonsSub = new HorizontalLayout();
		buttonsBody = new HorizontalLayout();
		FormLayout main = new FormLayout();
		setWidth(100, Unit.PERCENTAGE);
		setHeight(100, Unit.PERCENTAGE);
		setSpacing(true);
		name = new RequiredTextField(msg);
		if (editMode)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
		} else
			name.setValue(msg.getMessage("MessageTemplatesEditor.defaultName"));

		name.setCaption(msg.getMessage("MessageTemplatesEditor.name"));
		description = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.description"));
		consumer = new ComboBox(msg.getMessage("MessageTemplatesEditor.consumer"));
		Collection<MessageTemplateConsumer> consumers = registry.getAll();
		for (MessageTemplateConsumer c : consumers)
		{
			consumer.addItem(c.getName());
		}
		consumerDescription = new Label();
		
		subject = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.subject"));
		body = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.body"));
		
		if (editMode)
		{
			consumer.setValue(toEdit.getConsumer());
			description.setValue(toEdit.getDescription());
			System.out.println(toEdit.getAllMessages().keySet());
			subject.setValue(toEdit.getAllMessages().get(" ").getSubject());
			body.setValue(toEdit.getAllMessages().get(" ").getBody());
			setConsumerDesc();
		}

		
		
		consumer.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				setConsumerDesc();
			}

			
		});
		consumer.setImmediate(true);
		
		
		main.addComponents(name, description, consumer, consumerDescription, buttonsSub, subject, buttonsBody ,body);
		main.setSizeFull();
		addComponent(main);
	}

	public MessageTemplate getTemplate()
	{
		String n = name.getValue();
		String desc = description.getValue();
		String cons = consumer.getValue().toString();
		
		Map<String, Message> m = new HashMap<String, Message>();
		Message ms = new Message(subject.getValue(), body.getValue());
		m.put(" ", ms);

		return new MessageTemplate(n, desc, m, cons);

	}
	
	private void setConsumerDesc()
	{
		try
		{
			MessageTemplateConsumer con = registry.getByName(
					consumer.getValue().toString());
			consumerDescription.setValue(con.getDescription());
			updateVarButtons(con, subject, buttonsSub);
			updateVarButtons(con, body, buttonsBody);
		} catch (IllegalTypeException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private void updateVarButtons(MessageTemplateConsumer consumer, final AbstractTextField field, HorizontalLayout buttons)
	{
		buttons.removeAllComponents();
		for (Map.Entry<String, String> var:consumer.getVariables().entrySet())
		{
			final Button b = new Button();
			b.addStyleName(Reindeer.BUTTON_SMALL);
			b.setCaption(var.getKey());
			b.setDescription(var.getValue());
			b.addClickListener(new Button.ClickListener()
			{
				
				@Override
				public void buttonClick(ClickEvent event)
				{
					String val = field.getValue();
					int l = val.length();
					String s = val.substring(0,field.getCursorPosition());
					String f = val.substring(field.getCursorPosition());
					field.setValue(s + "{" +b.getCaption() + "}" + f);
					
				}
			});		
			buttons.addComponent(b);
		}
		
		
	}
	
	

}
