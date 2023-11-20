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
import jakarta.annotation.security.PermitAll;
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

import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition.CUSTOM_VAR_PREFIX;

@PermitAll
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
	private MessagesValidator bodyValidator;
	private SingleMessageValidator innerBodyValidator;
	private MessagesValidator subjectValidator;
	private SingleMessageValidator innerSubjectValidator;
	private Binder<MessageTemplate> binder;
	private Binder<I18nMessage> messageBinder;
	private Map<String, NotificationChannelInfo> notificationChannelsMap;
	private MultiSelectComboBox<String> customVariablesPicker;
	private BreadCrumbParameter breadCrumbParameter;

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
	public void setParameter(BeforeEvent event, @OptionalParameter String messageTemplateName)
	{
		getContent().removeAll();

		MessageTemplate messageTemplate;
		if (messageTemplateName == null)
		{
			messageTemplate = new MessageTemplate();
			messageTemplate.setMessage(new I18nMessage(new I18nString(), new I18nString()));
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			editMode = false;
		} else
		{
			messageTemplate = controller.getMessageTemplate(messageTemplateName);
			breadCrumbParameter = new BreadCrumbParameter(messageTemplateName, messageTemplateName);
			editMode = true;
		}
		initUI(messageTemplate);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void initUI(MessageTemplate toEdit)
	{
		subjectValidator = new MessagesValidator();
		innerSubjectValidator = new SingleMessageValidator(null, false);
		bodyValidator = new MessagesValidator();
		innerBodyValidator = new SingleMessageValidator(null, true);

		TextField name = new TextField();
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		name.setPlaceholder(msg.getMessage("MessageTemplatesEditor.defaultName"));

		TextField description = new TextField();
		description.setWidth(TEXT_FIELD_BIG.value());

		notificationChannels = new ComboBox<>();
		reloadNotificationChannels(EnumSet.noneOf(CommunicationTechnology.class));
		notificationChannels.setRequiredIndicatorVisible(true);
		notificationChannels.setWidth(TEXT_FIELD_MEDIUM.value());
		notificationChannels.setClassName("disable-required-indicator");

		consumer = new ComboBox<>();
		consumer.setWidth(TEXT_FIELD_MEDIUM.value());
		consumer.getStyle().set("text-wrap", "nowrap");
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

		LocalizedTextFieldDetails subject = new LocalizedTextFieldDetails(
				msg.getEnabledLocales().values(),
				msg.getLocale()
		);
		subject.setWidthFull();
		subject.addValuesChangeListener(focussedField::set);

		ComboBox<MessageType> messageType = new ComboBox<>();
		messageType.setItems(MessageType.values());
		messageType.setItemLabelGenerator(Enum::name);

		LocalizedTextAreaDetails body = new LocalizedTextAreaDetails(
				msg.getEnabledLocales().values(),
				msg.getLocale()
		);
		body.setValue(toEdit.getMessage().getBody().getLocalizedMap());
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
		customVariablesPicker.setWidth(TEXT_FIELD_BIG.value());

		FormLayout formLayout = createFormLayout(name, description, subject, messageType, body);
		getContent().add(new VerticalLayout(formLayout, createActionLayout(msg, editMode, MessageTemplatesView.class, this::onConfirm)));

		configBinder(name, description, subject, messageType, body);
		setBean(toEdit, name, consumers);
	}

	private FormLayout createFormLayout(TextField name, TextField description, LocalizedTextFieldDetails subject,
			ComboBox<MessageType> messageType, LocalizedTextAreaDetails body)
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

	private void configBinder(TextField name, TextField description, LocalizedTextFieldDetails subject,
			ComboBox<MessageType> messageType, LocalizedTextAreaDetails body)
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
		subject.setValidator(innerSubjectValidator);
		messageBinder.forField(subject)
				.asRequired(getRequiredValidatorTemplatesShownAware(subject))
				.withValidator(subjectValidator)
				.bind(i18nMessage -> i18nMessage.getSubject().getLocalizedMap(), (localizedValues, localizedValues2) -> localizedValues.setSubject(convert(localizedValues2)));
		body.setValidator(innerBodyValidator);
		messageBinder.forField(body)
				.asRequired(getRequiredValidatorTemplatesShownAware(body))
				.withValidator(bodyValidator)
				.bind(i18nMessage -> i18nMessage.getBody().getLocalizedMap(), (localizedValues, localizedValues2) -> localizedValues.setBody(convert(localizedValues2)));
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
		Map<String, String> collect = localizedValues.entrySet().stream()
				.filter(entry -> !entry.getValue().isEmpty())
				.collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue));
		i18nString.addAllValues(collect);
		return i18nString;
	}

	private void onConfirm()
	{
		MessageTemplate template = getTemplate();

		if (template == null)
			return;

		if (editMode)
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
		innerSubjectValidator.setEnabled(showTemplate);
		bodyValidator.setEnabled(showTemplate);
		innerBodyValidator.setEnabled(showTemplate);

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
		if (!binder.isValid() || !messageBinder.isValid())
		{
			binder.validate();
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
		this.consumer.setHelperText(msg.getMessage(consumer.getDescriptionKey()));
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
			button.setTooltipText(msg.getMessage(var.getValue().getDescriptionKey()));
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
			innerSubjectValidator.setConsumer(c);
			innerBodyValidator.setConsumer(c);
		}
	}

	private class SingleMessageValidator implements Validator<String>
	{
		private MessageTemplateDefinition c;
		private final boolean checkMandatory;
		private boolean enabled;

		SingleMessageValidator(MessageTemplateDefinition c, boolean checkMandatory)
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
		public ValidationResult apply(String value, ValueContext context)
		{
			if (!enabled)
				return ValidationResult.ok();
			if(value.isBlank())
				return ValidationResult.ok();
			try
			{
				MessageTemplateValidator.validateText(c, value, checkMandatory);
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

	private class MessagesValidator implements Validator<Map<Locale, String>>
	{
		private boolean enabled;

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
			if (value == null || value.values().stream().allMatch(String::isBlank))
				return ValidationResult.error(msg.getMessage("fieldRequired"));

			return ValidationResult.ok();
		}
	}

}
