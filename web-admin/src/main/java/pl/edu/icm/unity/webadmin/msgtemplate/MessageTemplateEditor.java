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
import pl.edu.icm.unity.msgtemplates.MessageTemplateValidator;
import pl.edu.icm.unity.server.registries.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.RequiredComboBox;
import pl.edu.icm.unity.webui.common.RequiredTextArea;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component to edit or add message template
 * 
 * @author P. Piernik
 * 
 */
public class MessageTemplateEditor extends FormLayout
{
	private UnityMessageSource msg;
	private MessageTemplateConsumersRegistry registry;
	private AbstractTextField name;
	private TextArea description;
	private TextArea subject;
	private TextArea body;
	private ComboBox consumer;
	private Label consumerDescription;
	private boolean editMode;
	private HorizontalLayout buttons;
	private boolean subjectEdited;
	private MessageValidator validator;

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
		name.setCaption(msg.getMessage("MessageTemplatesEditor.name"));
		name.setSizeFull();
		name.setValidationVisible(false);
		description = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.description"));
		consumer = new RequiredComboBox(msg.getMessage("MessageTemplatesEditor.consumer"), msg);
		consumer.setImmediate(true);
		consumer.setValidationVisible(false);
		consumer.setNullSelectionAllowed(false);
		Collection<MessageTemplateConsumer> consumers = registry.getAll();
		for (MessageTemplateConsumer c : consumers)
		{
			consumer.addItem(c.getName());
		}
		consumerDescription = new Label();
		consumerDescription.setReadOnly(true);
		subject = new RequiredTextArea(msg.getMessage("MessageTemplatesEditor.subject"), msg);
		subject.setImmediate(true);
		subject.setWidth(100, Unit.PERCENTAGE);
		subject.setValidationVisible(false);
		subject.setRows(1);
		body = new RequiredTextArea(msg.getMessage("MessageTemplatesEditor.body"), msg);
		body.setImmediate(true);
		body.setRows(17);
		body.setWidth(100, Unit.PERCENTAGE);
		body.setValidationVisible(false);
		validator = new MessageValidator(null, msg);
		subject.addValidator(validator);
		body.addValidator(validator);
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
				updateValidator();
				body.setComponentError(null);
				subject.setComponentError(null);
			}

		});

		if (editMode)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
			consumer.setValue(toEdit.getConsumer());
			description.setValue(toEdit.getDescription());
			// Using empty locale!
			Message ms = toEdit.getAllMessages().get("");
			if (ms != null)
			{
				subject.setValue(ms.getSubject());
				body.setValue(ms.getBody());
			}
			setMessageConsumerDesc();
		} else
		{
			name.setValue(msg.getMessage("MessageTemplatesEditor.defaultName"));
			if (consumer.size() > 0)
			{
				consumer.setValue(consumer.getItemIds().toArray()[0]);
			}
		}

		addComponents(name, description, consumer, consumerDescription, buttons, subject,
				body);
		setSpacing(true);
	}

	public MessageTemplate getTemplate()
	{
		if (!validate())
			return null;
		String n = name.getValue();
		String desc = description.getValue();
		String cons = getConsumer().getName();
		Map<String, Message> m = new HashMap<String, Message>();
		Message ms = new Message(subject.getValue(), body.getValue());
		// Using empty locale!
		m.put("", ms);
		return new MessageTemplate(n, desc, m, cons);
	}

	private void setMessageConsumerDesc()
	{
		MessageTemplateConsumer consumer = getConsumer();
		if (consumer == null)
			return;
		consumerDescription.setValue(consumer.getDescription());
		updateVarButtons(consumer);
	}

	private void updateVarButtons(MessageTemplateConsumer consumer)
	{
		buttons.removeAllComponents();
		for (Map.Entry<String, String> var : consumer.getVariables().entrySet())
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
					if (subjectEdited)
					{
						addVar(subject, b.getCaption());
					} else
					{
						addVar(body, b.getCaption());
					}
				}
			});
			buttons.addComponent(b);
		}

	}

	private MessageTemplateConsumer getConsumer()
	{

		String c = (String) consumer.getValue();
		if (c == null)
			return null;
		MessageTemplateConsumer consumer = null;
		try
		{
			consumer = registry.getByName(c);
		} catch (IllegalTypeException e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("MessageTemplatesEditor.errorConsumers"), e);
			return null;
		}
		return consumer;
	}

	private void addVar(AbstractTextField f, String val)
	{
		String v = f.getValue();
		String st = v.substring(0, f.getCursorPosition());
		String fi = v.substring(f.getCursorPosition());
		f.setValue(st + "${" + val + "}" + fi);
	}

	private void updateValidator()
	{
		MessageTemplateConsumer c = getConsumer();
		if (c != null)
			validator.setConsumer(c);
	}

	private boolean validate()
	{
		updateValidator();
		name.setValidationVisible(true);
		consumer.setValidationVisible(true);
		subject.setValidationVisible(true);
		subject.setValidationVisible(true);
		body.setValidationVisible(true);
		return name.isValid() && consumer.isValid() && subject.isValid() && body.isValid();

	}

	private class MessageValidator extends MessageTemplateValidator implements Validator
	{
		private UnityMessageSource msg;

		public MessageValidator(MessageTemplateConsumer consumer, UnityMessageSource msg)
		{
			super(consumer);
			this.msg = msg;
		}

		public void setConsumer(MessageTemplateConsumer consumer)
		{
			this.consumer = consumer;

		}

		@Override
		public void validate(Object value) throws InvalidValueException
		{
			if (!validateText(value.toString()))
			{
				throw new InvalidValueException(
						msg.getMessage("MessageTemplatesEditor.errorVars"));
			}
		}
	}

}
