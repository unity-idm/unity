/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.settings.msgTemplates;

import static pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition.CUSTOM_VAR_PREFIX;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msg_template.MessageTemplateVariable;
import pl.edu.icm.unity.base.msg_template.MessageType;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.notifications.NotificationChannelInfo;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator.IllegalVariablesException;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator.MandatoryVariablesException;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.CompatibleNotificationChannelsComboBox;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.widgets.DescriptionTextField;

/**
 * Component to edit or add message template
 * 
 * @author P. Piernik
 * 
 */
class MessageTemplateEditor extends CompactFormLayout
{
	private MessageSource msg;
	private MessageTemplateConsumersRegistry registry;
	private NotificationsManagement notChannelsMan;
	private TextField name;
	private DescriptionTextField description;
	private I18nTextField subject;
	private I18nTextArea body;
	private ComboBox<String> consumer;
	private CompatibleNotificationChannelsComboBox notificationChannels;
	private MessageTypeComboBox messageType;
	private Label consumerDescription;
	private boolean editMode;
	private HorizontalLayout buttons;
	private MessageValidator bodyValidator;
	private MessageValidator subjectValidator;
	private AbstractTextField focussedField;
	private MessageTemplateManagement msgTemplateMgr;
	private Binder<MessageTemplate> binder;
	private Binder<I18nMessage> messageBinder;
	private Map<String, NotificationChannelInfo> notificationChannelsMap;
	private boolean showTemplate;
	private Label externalTemplateInfo;
	private ChipsWithTextfield customVariablesPicker;

	MessageTemplateEditor(MessageSource msg,
			MessageTemplateConsumersRegistry registry, MessageTemplate toEdit,
			MessageTemplateManagement msgTemplateMgr,  NotificationsManagement notChannelsMan)
	{
		super();
		this.msgTemplateMgr = msgTemplateMgr;
		editMode = toEdit != null;
		this.msg = msg;
		this.registry = registry;
		this.notChannelsMan = notChannelsMan;
		initUI(toEdit);

	}
	
	private void initUI(MessageTemplate toEdit)
	{
		initNotificationChannels();
		
		buttons = new HorizontalLayout();
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setCaption(msg.getMessage("MessageTemplatesEditor.allowedVars"));

		name = new TextField(msg.getMessage("MessageTemplatesEditor.name"));
		name.setWidth(20, Unit.EM);
		
		description = new DescriptionTextField(msg);

		consumer = new ComboBox<>(msg.getMessage("MessageTemplatesEditor.consumer"));
		consumer.setEmptySelectionAllowed(false);
		consumer.setWidth(20, Unit.EM);
		Collection<String> consumers = registry.getAll().stream().map(c -> c.getName())
				.collect(Collectors.toList());
		consumer.setItems(consumers);
		consumerDescription = new Label();
		
		notificationChannels = new CompatibleNotificationChannelsComboBox(
				EnumSet.noneOf(CommunicationTechnology.class), notChannelsMan);
		notificationChannels.setCaption(msg.getMessage("MessageTemplatesEditor.notificationChannel"));
		notificationChannels.setEmptySelectionAllowed(false);
		notificationChannels.setRequiredIndicatorVisible(true);
		notificationChannels.addValueChangeListener(event -> {
			toggleSubjectAndBody(event.getValue());
		});
		notificationChannels.setWidth(20, Unit.EM);
		subject = new I18nTextField(msg, msg.getMessage("MessageTemplatesEditor.subject"));
		subject.setWidth(100, Unit.PERCENTAGE);
		body = new I18nTextArea(msg, msg.getMessage("MessageTemplatesEditor.body"), 8);

		messageType = new MessageTypeComboBox(msg, this::getBodyForPreview);
		subjectValidator = new MessageValidator(null, false);
		bodyValidator = new MessageValidator(null, true);

		focussedField = null;
		FocusListener focusListener = event -> {
			Component c = event.getComponent();
			if (c instanceof TextField)
				focussedField = (TextField) c;
			if (c instanceof TextArea)
				focussedField = (TextArea) c;
		};

		subject.addFocusListener(focusListener);
		body.addFocusListener(focusListener);

		consumer.addValueChangeListener(event -> {
			EnumSet<CommunicationTechnology> compatibleTechnologies = registry.getByName(event.getValue())
					.getCompatibleTechnologies();
			notificationChannels.reload(compatibleTechnologies);
			notificationChannels.setDefaultValue();
			notificationChannels.setVisible(!compatibleTechnologies.isEmpty());
			setMessageConsumerDesc();
			updateValidator();
			messageBinder.validate();
		});

		Label separator = new Label("");
		externalTemplateInfo = new Label(msg.getMessage("MessageTemplatesEditor.externalTemplateInfo"));
		externalTemplateInfo.setWidth(100, Unit.PERCENTAGE);
		externalTemplateInfo.setVisible(false);
		customVariablesPicker = new ChipsWithTextfield(msg);
		customVariablesPicker.setCaption(msg.getMessage("MessageTemplatesEditor.customVariables"));
		customVariablesPicker.setValidator(msg, str -> str.matches("[a-zA-Z0-9_\\-\\.]*"),
				msg.getMessage("MessageTemplatesEditor.customVariableIllegalCharsError"));
		
		addComponents(name, description, consumer, consumerDescription,
				notificationChannels, separator, buttons, subject, messageType,
				body, externalTemplateInfo, customVariablesPicker);

		binder = new Binder<>(MessageTemplate.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");
		binder.forField(consumer).asRequired(msg.getMessage("fieldRequired"))
				.bind("consumer");
		binder.forField(notificationChannels).withNullRepresentation("").withValidator((value, context) -> {
			EnumSet<CommunicationTechnology> compatibleTechnologies = registry
					.getByName(consumer.getValue()).getCompatibleTechnologies();
			if (compatibleTechnologies.isEmpty())
			{
				return ValidationResult.ok();
			} else if (value == null || value.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else if (notificationChannelsMap.get(value) == null)
			{
				return ValidationResult.error(msg.getMessage("MessageTemplatesEditor.undefinedChannel"));
			}

			return ValidationResult.ok();
		}).bind("notificationChannel");
		
		binder.forField(messageType).asRequired(msg.getMessage("fieldRequired"))
				.bind("type");
		messageBinder = new Binder<>(I18nMessage.class);
		messageBinder.forField(subject)
				.withValidator(subjectValidator)
				.asRequired(getRequiredValidatorTemplatesShownAware(subject))
				.bind("subject");
		messageBinder.forField(body)
				.asRequired(getRequiredValidatorTemplatesShownAware(body))
				.withValidator(bodyValidator)
				.bind("body");
		if (editMode)
		{
			name.setReadOnly(true);
			String channel = toEdit.getNotificationChannel();
			binder.setBean(toEdit);
			// Using empty locale!
			I18nMessage ms = toEdit.getMessage();
			if (ms != null)
			{
				messageBinder.setBean(ms);
			}
			notificationChannels.setValue(channel);
			setMessageConsumerDesc();
			
			customVariablesPicker.setItems(
					MessageTemplateValidator.extractCustomVariables(toEdit.getMessage()).stream()
						.map(var -> var.substring(CUSTOM_VAR_PREFIX.length()))
						.collect(Collectors.toList()));
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
	}

	private Validator<I18nString> getRequiredValidatorTemplatesShownAware(HasValue<?> field)
	{
		return Validator.from(value -> !showTemplate || !Objects.equals(value, field.getEmptyValue()),
		                    context -> msg.getMessage("fieldRequired"));
	}
	
	private void toggleSubjectAndBody(String channel)
	{
		
		NotificationChannelInfo notificationChannel = notificationChannelsMap.get(channel);
		showTemplate = !(notificationChannel != null && notificationChannel.isSupportingTemplates());
		subject.setVisible(showTemplate);
		body.setVisible(showTemplate);
		messageType.setVisible(showTemplate);
		subjectValidator.setEnabled(showTemplate);
		bodyValidator.setEnabled(showTemplate);
		
		externalTemplateInfo.setVisible(!showTemplate);
		customVariablesPicker.setVisible(!showTemplate);
	}
	
	private void initNotificationChannels()
	{
		try
		{
			notificationChannelsMap = notChannelsMan.getNotificationChannels();
		} catch (EngineException e)
		{
			throw new InternalException("Cannot get notification channels", e);
		}
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
	
	MessageTemplate getTemplate()
	{
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
		
		NotificationChannelInfo notificationChannel = notificationChannelsMap.get(msgTemplate.getNotificationChannel());
		if (notificationChannel != null && notificationChannel.isSupportingTemplates())
		{
			String customVariables = customVariablesPicker.getItems().stream()
					.map(variable -> "${custom." + variable + "}")
					.collect(Collectors.joining());
			msgTemplate.setMessage(new I18nMessage(new I18nString(), new I18nString(customVariables)));
		} else
		{
			I18nMessage message = messageBinder.getBean();
			//ensure to clean any values left from external template that could be used before.
			message.getBody().setDefaultValue("");
			msgTemplate.setMessage(message);
		}
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
			b.addStyleName(Styles.varPickerButton.toString());
			b.setCaption(var.getKey());
			b.setDescription(msg.getMessage(var.getValue().getDescriptionKey()));
			b.addClickListener(event -> 
			{
				if (focussedField != null)
					addVar(focussedField, b.getCaption());
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

	private void addVar(AbstractTextField focussedField2, String val)
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
		private boolean enabled;
		
		MessageValidator(MessageTemplateDefinition c, boolean checkMandatory)
		{
			this.c = c;
			this.checkMandatory = checkMandatory;
		}

		void setConsumer(MessageTemplateDefinition c)
		{
			this.c = c;
		}

		void setEnabled(boolean enabled)
		{
			this.enabled = enabled;
			
		}
		
		@Override
		public ValidationResult apply(I18nString value, ValueContext context)
		{
			if (!enabled)
				return ValidationResult.ok();

			if (context.getHasValue().isPresent() && context.getHasValue().get().isEmpty())
				ValidationResult.error(msg.getMessage("fieldRequired"));

			if (value == null)
				ValidationResult.error(msg.getMessage("fieldRequired"));
			if (value.isEmpty())
				ValidationResult.error(msg.getMessage("fieldRequired"));
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
