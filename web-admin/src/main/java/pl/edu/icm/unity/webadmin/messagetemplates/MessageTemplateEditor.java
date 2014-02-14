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
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component to edit or add message template
 * @author P. Piernik
 *
 */
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
	private HorizontalLayout buttons;
	private boolean editSubject;

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
		
		buttons = new HorizontalLayout();
		FormLayout main = new FormLayout();
		setWidth(100, Unit.PERCENTAGE);
		setHeight(100, Unit.PERCENTAGE);
		setSpacing(true);
		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("MessageTemplatesEditor.name"));
		name.setSizeFull();
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
		
		editSubject = true;
		subject.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focus(FocusEvent event)
			{
				editSubject = true;
				
			}
		});
		subject.setImmediate(true);
		
		body.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focus(FocusEvent event)
			{
				editSubject = false;
				
			}
		});
		body.setImmediate(true);
		
		consumer.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				setConsumerDesc();
			}

			
		});
		consumer.setImmediate(true);
		
		if (editMode)
		{
			consumer.setValue(toEdit.getConsumer());
			description.setValue(toEdit.getDescription());
			if (toEdit.getAllMessages().get("") != null)
			{
				subject.setValue(toEdit.getAllMessages().get("").getSubject());
				body.setValue(toEdit.getAllMessages().get("").getBody());		
			}
			setConsumerDesc();
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
		} else
			name.setValue(msg.getMessage("MessageTemplatesEditor.defaultName"));
		
		main.addComponents(name, description, consumer, consumerDescription, buttons, subject, body);
		main.setSizeFull();
		addComponent(main);
	}

	public MessageTemplate getTemplate()
	{
		String n = name.getValue();
		String desc = description.getValue();
		String cons = null;
		if (consumer.getValue() != null)
		{
			 cons = consumer.getValue().toString();	
		}
		Map<String, Message> m = new HashMap<String, Message>();
		Message ms = new Message(subject.getValue(), body.getValue());
		m.put("", ms);
		return new MessageTemplate(n, desc, m, cons);
	}
	
	private void setConsumerDesc()
	{
		if (consumer.getValue() == null)
		{
			return;
		}
		try
		{

			MessageTemplateConsumer con = registry.getByName(consumer.getValue()
					.toString());
			consumerDescription.setValue(con.getDescription());
			updateVarButtons(con);

		} catch (IllegalTypeException e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("MessageTemplatesEditor.errorConsumers"), e);
		}
	}
	
	private void updateVarButtons(MessageTemplateConsumer consumer)
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
					if(editSubject)
					{
						addVar(subject, b.getCaption());
					}else
					{
						addVar(body, b.getCaption());
					}		
				}
			});		
			buttons.addComponent(b);
		}
		
		
	}
	
	private void addVar(AbstractTextField f,String val)
	{
		String v = f.getValue();
		String st = v.substring(0,f.getCursorPosition());
		String fi = v.substring(f.getCursorPosition());
		f.setValue(st + "{" + val + "}" + fi);
	}

}
