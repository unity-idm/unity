/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.msgtemplates.MessageTemplateConsumer;
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
import com.vaadin.ui.themes.Reindeer;

/**
 * Component to edit or add message template
 * @author P. Piernik
 *
 */
public class MessageTemplateEditor extends FormLayout
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
	private boolean subjectEdited;

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
		buttons.setSpacing(false);
		buttons.setMargin(false);
		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("MessageTemplatesEditor.name") + ":");
		name.setSizeFull();
		description = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.description") + ":");
		consumer = new ComboBox(msg.getMessage("MessageTemplatesEditor.consumer") + ":");
		consumer.setImmediate(true);
		consumer.setRequired(true);
		Collection<MessageTemplateConsumer> consumers = registry.getAll();
		for (MessageTemplateConsumer c : consumers)
		{
			consumer.addItem(c.getName());
		}
		consumerDescription = new Label();
	
		
		subject = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.subject") + ":");
		subject.setImmediate(true);
		subject.setRequired(true);
		body = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.body") + ":");
		body.setImmediate(true);
		body.setRequired(true);
		subjectEdited = true;
		subject.addFocusListener(new FocusListener()
		{	
			@Override
			public void focus(FocusEvent event)
			{
				subjectEdited = true;
				
			}
		});
		body.addFocusListener(new FocusListener()
		{	
			@Override
			public void focus(FocusEvent event)
			{
				subjectEdited = false;
				
			}
		});
		
		consumer.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				setMessageConsumerDesc();
			}

			
		});
			
		if (editMode)
		{	
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
			consumer.setValue(toEdit.getConsumer());
			description.setValue(toEdit.getDescription());
			Message ms = toEdit.getAllMessages().get("");
			if (ms != null)
			{
				subject.setValue(ms.getSubject());
				body.setValue(ms.getBody());		
			}
			setMessageConsumerDesc();	
		} else
			name.setValue(msg.getMessage("MessageTemplatesEditor.defaultName"));
		
		addComponents(name, description, consumer, consumerDescription, buttons, subject, body);
		setSizeFull();
		setSpacing(true);
		
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
//	 	Using empty locale!
		m.put("", ms);
		return new MessageTemplate(n, desc, m, cons);
	}
	
	private void setMessageConsumerDesc()
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
					if(subjectEdited)
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
		f.setValue(st + "${" + val + "}" + fi);
	}

}
