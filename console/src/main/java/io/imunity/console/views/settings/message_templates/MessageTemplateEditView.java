/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.*;
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

import javax.annotation.security.PermitAll;
import java.util.*;
import java.util.stream.Collectors;

import static pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition.CUSTOM_VAR_PREFIX;

@PermitAll
@Breadcrumb(key = "edit")
@Route(value = "/message-templates/edit", layout = ConsoleMenu.class)
public class MessageTemplateEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final MessageTemplateController controller;
	private final NotificationPresenter notificationPresenter;
	private final MessageTemplateConsumersRegistry registry;
	private final MessageTemplateManagement msgTemplateMgr;
	private final NotificationsManagement notChannelsMan;

	private final FocusedField focussedField = new FocusedField();
	private final HorizontalLayout buttons = new HorizontalLayout();

	private ComboBox<String> consumer;
	private ComboBox<String> notificationChannels;
	private boolean editMode;
	private MessageValidator bodyValidator;
	private MessageValidator subjectValidator;
	private Binder<MessageTemplate> binder;
	private Binder<I18nMessage> messageBinder;
	private Map<String, NotificationChannelInfo> notificationChannelsMap;
	private MultiSelectComboBox<String> customVariablesPicker;
	MessageTemplateEditView(MessageSource msg, MessageTemplateController controller,
								   NotificationPresenter notificationPresenter, MessageTemplateConsumersRegistry registry,
							       MessageTemplateManagement msgTemplateMgr,
								   NotificationsManagement notChannelsMan)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		this.registry = registry;
		this.notChannelsMan = notChannelsMan;
		this.msgTemplateMgr = msgTemplateMgr;
		initNotificationChannels();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String messageTemplateName) {
		getContent().removeAll();

		MessageTemplate messageTemplate;
		if(messageTemplateName == null)
		{
			messageTemplate = new MessageTemplate();
			messageTemplate.setMessage(new I18nMessage(new I18nString(), new I18nString()));
			editMode = false;
		}
		else
		{
			messageTemplate = controller.getMessageTemplate(messageTemplateName);
			editMode = true;
		}
		initUI(messageTemplate);
	}

	private void initUI(MessageTemplate toEdit)
	{
		subjectValidator = new MessageValidator(null, false);
		bodyValidator = new MessageValidator(null, true);

		TextField name = new TextField();
		name.setWidth("var(--vaadin-text-field-medium)");

		TextField description = new TextField();
		description.setWidth("var(--vaadin-text-field-big)");

		notificationChannels = new ComboBox<>();
		reloadNotificationChannels(EnumSet.noneOf(CommunicationTechnology.class));
		notificationChannels.setRequiredIndicatorVisible(true);
		notificationChannels.setWidth("var(--vaadin-text-field-medium)");
		notificationChannels.setClassName("disable-required-indicator");

		consumer = new ComboBox<>();
		consumer.setWidth("var(--vaadin-text-field-medium)");
		Collection<String> consumers = registry.getAll().stream()
				.map(MessageTemplateDefinition::getName)
				.collect(Collectors.toList());
		consumer.setItems(consumers);
		consumer.addValueChangeListener(event ->
		{
			EnumSet<CommunicationTechnology> compatibleTechnologies = registry.getByName(event.getValue())
					.getCompatibleTechnologies();
			reloadNotificationChannels(compatibleTechnologies);
			notificationChannels.setVisible(!compatibleTechnologies.isEmpty());
			setMessageConsumerDesc();
			updateValidator();
			messageBinder.validate();
		});

		LocaleTextFieldDetails subject = new LocaleTextFieldDetails(new HashSet<>(msg.getEnabledLocales().values()), msg.getLocale(), "", locale -> Optional.ofNullable(toEdit.getMessage().getSubject().getValueRaw(locale.getLanguage())).orElse(""));
		subject.addValuesChangeListener(focussedField::set);
		subject.setWidthFull();

		ComboBox<MessageType> messageType = new ComboBox<>();
		messageType.setItems(MessageType.values());
		messageType.setItemLabelGenerator(Enum::name);

		LocaleTextAreaDetails body = new LocaleTextAreaDetails(new HashSet<>(msg.getEnabledLocales().values()), msg.getLocale(), "", locale -> Optional.ofNullable(toEdit.getMessage().getBody().getValueRaw(locale.getLanguage())).orElse(""));
		body.setWidthFull();
		body.addValuesChangeListener(focussedField::set);

		customVariablesPicker = new MultiSelectComboBox<>(msg.getMessage("MessageTemplatesEditor.externalTemplateInfo"));
		customVariablesPicker.setAllowCustomValue(true);
		customVariablesPicker.addCustomValueSetListener(event ->
		{
			HashSet<String> values = new HashSet<>(customVariablesPicker.getValue());
			values.add(event.getDetail());
			customVariablesPicker.setItems(values);
			customVariablesPicker.setValue(values);
		});
		customVariablesPicker.setWidth("var(--vaadin-text-field-big)");

		FormLayout formLayout = createFormLayout(name, description, subject, messageType, body);
		getContent().add(new VerticalLayout(formLayout, createActionLayout()));

		configBinder(name, description, subject, messageType, body);
		setBean(toEdit, name, consumers);
	}

	private FormLayout createFormLayout(TextField name, TextField description, LocaleTextFieldDetails subject, ComboBox<MessageType> messageType, LocaleTextAreaDetails body)
	{
		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		formLayout.addFormItem(name, msg.getMessage("MessageTemplatesEditor.name"));
		formLayout.addFormItem(description, msg.getMessage("MessageTemplateViewer.description"));
		formLayout.addFormItem(consumer, msg.getMessage("MessageTemplatesEditor.consumer"));
		formLayout.addFormItem(notificationChannels, msg.getMessage("MessageTemplatesEditor.notificationChannel"));
		formLayout.addFormItem(buttons, msg.getMessage("MessageTemplatesEditor.allowedVars"));
		FormLayout.FormItem subjectFormItem = formLayout.addFormItem(subject, msg.getMessage("MessageTemplatesEditor.subject"));
		FormLayout.FormItem messageTypeFormItem = formLayout.addFormItem(messageType, msg.getMessage("MessageTemplatesEditor.bodyType"));
		messageTypeFormItem.add(new LocaleButtonsBar(msg.getEnabledLocales().values(), msg.getMessage("MessageTemplateViewer.preview"),
				l -> () -> new PreviewDialog(getBodyForPreview(l), messageType.getValue().equals(MessageType.HTML)).open()));
		FormLayout.FormItem bodyFormItem = formLayout.addFormItem(body, msg.getMessage("MessageTemplatesEditor.body"));
		FormLayout.FormItem customVariablesPickerItem = formLayout.addFormItem(customVariablesPicker, msg.getMessage("MessageTemplatesEditor.customVariables"));
		notificationChannels.addValueChangeListener(event -> toggleSubjectAndBody(event.getValue(), Set.of(customVariablesPickerItem), Set.of(subjectFormItem, messageTypeFormItem, bodyFormItem)));
		return formLayout;
	}

	private void setBean(MessageTemplate toEdit, TextField name, Collection<String> consumers)
	{
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
			List<String> items = MessageTemplateValidator.extractCustomVariables(toEdit.getMessage()).stream()
					.map(var -> var.substring(CUSTOM_VAR_PREFIX.length()))
					.collect(Collectors.toList());
			customVariablesPicker.setItems(items);
			customVariablesPicker.setValue(items);
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
			messageBinder.setBean(new I18nMessage(new I18nString(), new I18nString()));
		}
	}

	private void configBinder(TextField name, TextField description, LocaleTextFieldDetails subject, ComboBox<MessageType> messageType, LocaleTextAreaDetails body)
	{
		binder = new Binder<>(MessageTemplate.class);
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(MessageTemplate::getName, MessageTemplate::setName);
		binder.forField(description)
				.bind(MessageTemplate::getDescription, MessageTemplate::setDescription);
		binder.forField(customVariablesPicker)
						.withValidator(
								str -> str.stream().allMatch(val -> val.matches("[a-zA-Z0-9_\\-.]*")),
								msg.getMessage("MessageTemplatesEditor.customVariableIllegalCharsError")
						).bind(ignore -> null, (x, y) -> {});
		binder.forField(consumer)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(MessageTemplate::getConsumer, MessageTemplate::setConsumer);
		binder.forField(notificationChannels)
				.withNullRepresentation("")
				.withValidator((value, context) ->
				{
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
				}
			).bind(MessageTemplate::getNotificationChannel, MessageTemplate::setNotificationChannel);
		binder.forField(messageType)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(MessageTemplate::getType, MessageTemplate::setType);

		messageBinder = new Binder<>(I18nMessage.class);
		messageBinder.forField(subject)
				.withValidator(subjectValidator)
				.asRequired(getRequiredValidatorTemplatesShownAware(subject))
				.bind(i18nMessage -> i18nMessage.getSubject().getLocalizedMap(), (localizedValues, localizedValues2) -> localizedValues.setSubject(convert(localizedValues2)));
		messageBinder.forField(body)
				.asRequired(getRequiredValidatorTemplatesShownAware(body))
				.withValidator(bodyValidator)
				.bind(i18nMessage -> i18nMessage.getBody().getLocalizedMap(), (localizedValues, localizedValues2) -> localizedValues.setBody(convert(localizedValues2)));
	}

	private HorizontalLayout createActionLayout()
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(MessageTemplatesView.class));
		Button updateButton = new Button(msg.getMessage("update"));
		updateButton.addClickListener(event -> onConfirm());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout horizontalLayout = new HorizontalLayout(cancelButton, updateButton);
		return horizontalLayout;
	}

	public void reloadNotificationChannels(EnumSet<CommunicationTechnology> supportedTechnologies)
	{
		if (supportedTechnologies.isEmpty())
		{
			notificationChannels.setItems(List.of());
			return;
		}

		Map<String, NotificationChannelInfo> channels;
		try
		{
			channels = notChannelsMan.getNotificationChannelsForTechnologies(supportedTechnologies);
		} catch (EngineException e)
		{
			throw new InternalException("Cannot get notification channels", e);
		}
		notificationChannels.setItems(channels.keySet());
		channels.keySet().stream().findFirst().ifPresent(notificationChannels::setValue);
	}

	private I18nString convert(Map<Locale, String> localizedValues)
	{
		I18nString i18nString = new I18nString();
		i18nString.addAllValues(localizedValues.entrySet().stream().collect(Collectors.toMap(x -> x.getKey().toString(), Map.Entry::getValue)));
		return i18nString;
	}

	private void onConfirm()
	{
		MessageTemplate template = getTemplate();

		if (template == null)
			return;

		if(editMode)
			controller.updateMessageTemplate(template);
		else
			controller.addMessageTemplate(template);

		UI.getCurrent().navigate(MessageTemplatesView.class);
	}

	private Validator<Map<Locale, String>> getRequiredValidatorTemplatesShownAware(HasValue<?, ?> field)
	{
		return Validator.from(value -> !Objects.equals(value, field.getEmptyValue()),
				context -> msg.getMessage("fieldRequired"));
	}

	private void toggleSubjectAndBody(String channel,
									  Set<FormLayout.FormItem> notTemplateItems,
									  Set<FormLayout.FormItem> templateItems)
	{

		NotificationChannelInfo notificationChannel = notificationChannelsMap.get(channel);
		boolean showTemplate = !(notificationChannel != null && notificationChannel.isSupportingTemplates());
		templateItems.forEach(item -> item.setVisible(showTemplate));
		subjectValidator.setEnabled(showTemplate);
		bodyValidator.setEnabled(showTemplate);

		notTemplateItems.forEach(item -> item.setVisible(!showTemplate));
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

	private String getBodyForPreview(Locale locale)
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
			return "Broken template: " + e;
		}

		String value = tplPreprocessed.getMessage().getBody().getValue(locale.getLanguage(), null);
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
			String customVariables = customVariablesPicker.getSelectedItems().stream()
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
		notificationChannels.setLabel(msg.getMessage(consumer.getDescriptionKey()));
		updateVarButtons(consumer);
	}

	private void updateVarButtons(MessageTemplateDefinition consumer)
	{
		buttons.removeAll();
		for (Map.Entry<String, MessageTemplateVariable> var : consumer.getVariables().entrySet())
		{
			Button button = new Button();
			button.setText(var.getKey());
			button.addThemeVariants(ButtonVariant.LUMO_SMALL);
			button.getElement().setProperty("title", msg.getMessage(var.getValue().getDescriptionKey()));
			button.addClickListener(event ->
			{
				if (focussedField.isSet())
					addVar(var.getKey());
			});
			buttons.add(button);
		}
	}

	private MessageTemplateDefinition getConsumer()
	{

		String c = consumer.getValue();
		MessageTemplateDefinition consumer;
		try
		{
			consumer = registry.getByName(c);
		} catch (IllegalArgumentException e)
		{
			notificationPresenter.showError(msg.getMessage("MessageTemplatesEditor.errorConsumers"), e.getMessage());
			return null;
		}
		return consumer;
	}

	private void addVar(String val)
	{
		String v = focussedField.getValue();
		String st = v.substring(0, focussedField.getCursorPosition());
		String fi = v.substring(focussedField.getCursorPosition());
		focussedField.setValue(st + "${" + val + "}" + fi);
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

	private class MessageValidator implements Validator<Map<Locale, String>>
	{
		private MessageTemplateDefinition c;
		private final boolean checkMandatory;
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
		public ValidationResult apply(Map<Locale, String> value, ValueContext context)
		{
			if (!enabled)
				return ValidationResult.ok();

			if (context.getHasValue().isPresent() && context.getHasValue().get().isEmpty())
				return ValidationResult.error(msg.getMessage("fieldRequired"));

			if (value == null)
				return  ValidationResult.error(msg.getMessage("fieldRequired"));
			if (value.values().stream().allMatch(String::isBlank))
				return  ValidationResult.error(msg.getMessage("fieldRequired"));
			try
			{
				MessageTemplateValidator.validateText(c, value.toString(), checkMandatory);
			} catch (MessageTemplateValidator.IllegalVariablesException e)
			{
				return ValidationResult.error(msg.getMessage("MessageTemplatesEditor.errorUnknownVars" ,
						e.getUnknown().toString()));

			} catch (MessageTemplateValidator.MandatoryVariablesException e)
			{
				return ValidationResult.error(msg.getMessage("MessageTemplatesEditor.errorMandatoryVars",
						e.getMandatory().toString()));
			}

			return ValidationResult.ok();
		}
	}

}
