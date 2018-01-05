/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator.IllegalVariablesException;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator.MandatoryVariablesException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.DescriptionTextArea2;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidator;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea2;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField2;

/**
 * Component to edit or add message template
 * 
 * @author P. Piernik
 * 
 */
public class MessageTemplateEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private MessageTemplateConsumersRegistry registry;
	private TextField name;
	private TextArea description;
	private I18nTextField2 subject;
	private I18nTextArea2 body;
	private ComboBox<String> consumer;
	private MessageTypeComboBox messageType;
	private Label consumerDescription;
	private boolean editMode;
	private HorizontalLayout buttons;
	private MessageValidator bodyValidator;
	private MessageValidator subjectValidator;
	private TextArea focussedField;
	private MessageTemplateManagement msgTemplateMgr;
	private Binder<MessageTemplate> binder;
	private Binder<I18nMessage> messageBinder;
	private FormValidator validator;

	public MessageTemplateEditor(UnityMessageSource msg,
			MessageTemplateConsumersRegistry registry, MessageTemplate toEdit,
			MessageTemplateManagement msgTemplateMgr)
	{
		super();
		this.msgTemplateMgr = msgTemplateMgr;
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

		name = new TextField(msg.getMessage("MessageTemplatesEditor.name"));

		description = new DescriptionTextArea2(
				msg.getMessage("MessageTemplatesEditor.description"));

		consumer = new ComboBox<>(msg.getMessage("MessageTemplatesEditor.consumer"));
		consumer.setEmptySelectionAllowed(false);
		Collection<String> consumers = registry.getAll().stream().map(c -> c.getName())
				.collect(Collectors.toList());
		consumer.setItems(consumers);
		consumerDescription = new Label();
		
		subject = new I18nTextField2(msg, msg.getMessage("MessageTemplatesEditor.subject"));
		body = new I18nTextArea2(msg, msg.getMessage("MessageTemplatesEditor.body"), 8);

		messageType = new MessageTypeComboBox(msg, this::getBodyForPreview);
		subjectValidator = new MessageValidator(null, false);
		bodyValidator = new MessageValidator(null, true);

		focussedField = null;
		FocusListener focusListener = event -> {
			Component c = event.getComponent();
			if (c instanceof TextArea)
				focussedField = (TextArea) c;
		};

		subject.addFocusListener(focusListener);
		body.addFocusListener(focusListener);

		consumer.addValueChangeListener(event -> {
			setMessageConsumerDesc();
			updateValidator();
			messageBinder.validate();
		});
		
		addComponents(name, description, consumer, consumerDescription, buttons, subject,
				messageType, body);
		
		binder = new Binder<>(MessageTemplate.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");
		binder.forField(consumer).asRequired(msg.getMessage("fieldRequired"))
				.bind("consumer");
		binder.forField(messageType).asRequired(msg.getMessage("fieldRequired"))
				.bind("type");
		messageBinder = new Binder<>(I18nMessage.class);
		messageBinder.forField(subject).withValidator(subjectValidator)
				.asRequired(msg.getMessage("fieldRequired")).bind("subject");
		messageBinder.forField(body).withValidator(bodyValidator)
				.asRequired(msg.getMessage("fieldRequired")).bind("body");
		if (editMode)
		{
			name.setReadOnly(true);
			binder.setBean(toEdit);
			// Using empty locale!
			I18nMessage ms = toEdit.getMessage();
			if (ms != null)
			{
				messageBinder.setBean(ms);
			}
			setMessageConsumerDesc();
		} else
		{
			MessageTemplate msgTemplate = new MessageTemplate();
			msgTemplate.setName(msg.getMessage("MessageTemplatesEditor.defaultName"));
			if (!consumers.isEmpty())
			{
				msgTemplate.setConsumer(consumers.iterator().next());
			}
			msgTemplate.setType(MessageType.PLAIN);
			msgTemplate.setDescription("");
			binder.setBean(msgTemplate);
			messageBinder.setBean(new I18nMessage());
		}
		
		setSpacing(true);
		validator = new FormValidator(this);
	}

	private String getBodyForPreview(String locale)
	{
		MessageTemplate tpl = getTemplate();
		if (tpl == null)
			return "Message template is invalid";
		
		MessageTemplate tplPreprocessed;
		try
		{
			tplPreprocessed = msgTemplateMgr.getPreprocessedTemplate(tpl);
		} catch (EngineException e)
		{
			return "Broken template: " + e.toString();
		}
		
		String value = tplPreprocessed.getMessage().getBody().getValue(locale, null);
		return value != null ? value : "";
	}
	
	public MessageTemplate getTemplate()
	{
		try
		{
			validator.validate();
		} catch (FormValidationException e)
		{
			return null;
		}
		
		if (!binder.isValid())
		{	
			binder.validate();
			return null;
		}
		if (!messageBinder.isValid())
		{
			messageBinder.validate();
			return null;
		}
		MessageTemplate msgTemplate = binder.getBean();
		msgTemplate.setMessage(messageBinder.getBean());
		return msgTemplate;
	}

	private void setMessageConsumerDesc()
	{
		MessageTemplateDefinition consumer = getConsumer();
		if (consumer == null)
			return;
		consumerDescription.setValue(msg.getMessage(consumer.getDescriptionKey()));
		updateVarButtons(consumer);
	}

	private void updateVarButtons(MessageTemplateDefinition consumer)
	{
		buttons.removeAllComponents();
		for (Map.Entry<String, MessageTemplateVariable> var : consumer.getVariables().entrySet())
		{
			final Button b = new Button();
			b.addStyleName(Styles.vButtonSmall.toString());
			b.setCaption(var.getKey());
			b.setDescription(msg.getMessage(var.getValue().getDescriptionKey()));
			b.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					if (focussedField != null)
						addVar(focussedField, b.getCaption());
				}
			});
			buttons.addComponent(b);
		}
	}

	private MessageTemplateDefinition getConsumer()
	{

		String c = (String) consumer.getValue();
		MessageTemplateDefinition consumer = null;
		try
		{
			consumer = registry.getByName(c);
		} catch (IllegalArgumentException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("MessageTemplatesEditor.errorConsumers"), e);
			return null;
		}
		return consumer;
	}

	private void addVar(TextArea focussedField2, String val)
	{
		String v = focussedField2.getValue();
		String st = v.substring(0, focussedField2.getCursorPosition());
		String fi = v.substring(focussedField2.getCursorPosition());
		focussedField2.setValue(st + "${" + val + "}" + fi);
	}

	private void updateValidator()
	{
		MessageTemplateDefinition c = getConsumer();
		if (c != null)
		{
			subjectValidator.setConsumer(c);
			bodyValidator.setConsumer(c);
		}
	}
	
	private class MessageValidator implements com.vaadin.data.Validator<I18nString>
	{
		private MessageTemplateDefinition c;
		private boolean checkMandatory;
		
		public MessageValidator(MessageTemplateDefinition c, boolean checkMandatory)
		{
			this.c = c;
			this.checkMandatory = checkMandatory;
		}

		public void setConsumer(MessageTemplateDefinition c)
		{
			this.c = c;
		}

		@Override
		public ValidationResult apply(I18nString value, ValueContext context)
		{
			try
			{
				MessageTemplateValidator.validateText(c, value.toString(), checkMandatory);
			} catch (IllegalVariablesException e)
			{
				return ValidationResult.error(msg.getMessage("MessageTemplatesEditor.errorUnknownVars" ,
						e.getUnknown().toString()));
			
			} catch (MandatoryVariablesException e)
			{
				return ValidationResult.error(msg.getMessage("MessageTemplatesEditor.errorMandatoryVars", 
						e.getMandatory().toString()));
			}
		
			return ValidationResult.ok();
		}
	}
}
