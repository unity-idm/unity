/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;


import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ListOfElementsWithActions;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.pass.MobilePasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings.ConfirmationMode;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Collections;
import java.util.List;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.*;
import static io.imunity.vaadin.endpoint.common.plugins.attributes.components.ListOfElementsWithActions.ActionColumn.Position.Left;

class PasswordCredentialResetSettingsEditor
{
	private final MessageSource msg;
	private final PasswordCredentialResetSettings initial;
	private final MessageTemplateManagement msgTplMan;
	private Checkbox enable;
	private IntegerField codeLength;
	private Checkbox requireQuestionConfirmation;
	private TextField questionAdder;
	private CompatibleTemplatesComboBox emailCodeMessageTemplate;
	private CompatibleTemplatesComboBox mobileCodeMessageTemplate;
	private Icon addIcon;
	private ListOfElementsWithActions<String> questions;
	private Select<ConfirmationMode> confirmationMode;
	
	private Binder<PasswordCredentialResetSettings> binder;
	
	PasswordCredentialResetSettingsEditor(MessageSource msg, MessageTemplateManagement msgTplMan,
			PasswordCredentialResetSettings initial)
	{
		this.msg = msg;
		this.initial = initial;
		this.msgTplMan = msgTplMan;
	}
	
	public void addViewerToLayout(FormLayout parent)
	{
		Span status = new Span(initial.isEnabled() ? msg.getMessage("yes") : msg.getMessage("no"));
		parent.addFormItem(status, msg.getMessage("PasswordCredentialResetSettings.enableRo"));
		if (!initial.isEnabled())
			return;
		
		Span codeLength = new Span(String.valueOf(initial.getCodeLength()));
		parent.addFormItem(codeLength, msg.getMessage("PasswordCredentialResetSettings.codeLength"));

		Span confirmationMode = new Span(
				msg.getMessage("PasswordCredentialResetSettings.confirmationMode"
						+ initial.getConfirmationMode().toString()));
		parent.addFormItem(confirmationMode, msg.getMessage("PasswordCredentialResetSettings.confirmationMode"));
		
		Span emailCodeTemplate = new Span(initial.getEmailSecurityCodeMsgTemplate());
		parent.addFormItem(emailCodeTemplate, msg.getMessage("PasswordCredentialResetSettings.emailMessageTemplate"));

		Span mobileCodeTemplate = new Span(initial.getMobileSecurityCodeMsgTemplate());
		parent.addFormItem(mobileCodeTemplate, msg.getMessage("PasswordCredentialResetSettings.mobileMessageTemplate"));

		Span requireQuestionConfirmation = new Span(initial.isRequireSecurityQuestion() ?
				msg.getMessage("yes") : msg.getMessage("no"));
		parent.addFormItem(requireQuestionConfirmation, msg.getMessage("PasswordCredentialResetSettings.requireQuestionConfirmation"));
		
		if (!initial.isRequireSecurityQuestion())
			return;
		
		Span questions = new Span(String.valueOf(initial.getQuestions().get(0)));
		parent.addFormItem(questions, msg.getMessage("PasswordCredentialResetSettings.questions"));

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setPadding(false);
		for (int i=1; i<initial.getQuestions().size(); i++)
			verticalLayout.add(new Span(initial.getQuestions().get(i)));
		parent.addFormItem(verticalLayout, "");
	}
	
	public void addEditorToLayout(FormLayout parent)
	{
		initUI();
		setValue(initial);
		parent.addFormItem(enable, "");
		parent.addFormItem(codeLength, msg.getMessage("PasswordCredentialResetSettings.codeLength"));
		parent.addFormItem(confirmationMode, msg.getMessage("PasswordCredentialResetSettings.confirmationMode"));
		parent.addFormItem(emailCodeMessageTemplate, msg.getMessage("PasswordCredentialResetSettings.emailMessageTemplate"));
		parent.addFormItem(mobileCodeMessageTemplate, msg.getMessage("PasswordCredentialResetSettings.mobileMessageTemplate"));
		parent.addFormItem(requireQuestionConfirmation, "");
		parent.addFormItem(questionAdder, msg.getMessage("PasswordCredentialResetSettings.defineNewQuestion"))
				.add(addIcon);
		parent.addFormItem(questions, "");
	}
	
	private void initUI()
	{
		
		binder = new Binder<>(PasswordCredentialResetSettings.class);

		confirmationMode = new Select<>();
		confirmationMode.setItems(ConfirmationMode.values());
		confirmationMode.addValueChangeListener(e->
		{
			emailCodeMessageTemplate.setEnabled(getEmailMessageTemplateState());
			mobileCodeMessageTemplate.setEnabled(getMobileMessageTemplateState());
		});
		
		confirmationMode.setItemLabelGenerator(i -> msg.getMessage("PasswordCredentialResetSettings.confirmationMode" + i.toString()));
		confirmationMode.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(confirmationMode).bind("confirmationMode");
		
		enable = new Checkbox(msg.getMessage("PasswordCredentialResetSettings.enable"));
		enable.addValueChangeListener(event -> setEnabled(enable.getValue()));
		binder.forField(enable).bind("enabled");
			
		codeLength = new IntegerField();
		codeLength.setMin(2);
		codeLength.setMax(10);
		codeLength.setStepButtonsVisible(true);
		binder.forField(codeLength).asRequired().bind("codeLength");
				
		requireQuestionConfirmation = new Checkbox(
				msg.getMessage("PasswordCredentialResetSettings.requireQuestionConfirmation"));
		
		emailCodeMessageTemplate = new CompatibleTemplatesComboBox(EmailPasswordResetTemplateDef.NAME, msgTplMan);
		emailCodeMessageTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(emailCodeMessageTemplate).asRequired((v, c) -> ((v == null || v.isEmpty()) && getEmailMessageTemplateState())
				? ValidationResult.error(msg.getMessage("fieldRequired")) : ValidationResult.ok())
				.bind("emailSecurityCodeMsgTemplate");
		emailCodeMessageTemplate.setEnabled(false);
		
		mobileCodeMessageTemplate = new CompatibleTemplatesComboBox(MobilePasswordResetTemplateDef.NAME, msgTplMan);
		mobileCodeMessageTemplate.setEnabled(false);
		mobileCodeMessageTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(mobileCodeMessageTemplate).asRequired((v, c) -> ((v == null || v.isEmpty()) && getMobileMessageTemplateState())
				? ValidationResult.error(msg.getMessage("fieldRequired")) : ValidationResult.ok())
				.bind("mobileSecurityCodeMsgTemplate");
		
		requireQuestionConfirmation.addValueChangeListener(event -> {
			boolean state = requireQuestionConfirmation.getValue();
			questionAdder.setEnabled(state);
			questions.setEnabled(state);
			if(!state)
				addIcon.addClassName(DISABLED_ICON.getName());
			else
				addIcon.removeClassName(DISABLED_ICON.getName());
		});
		binder.forField(requireQuestionConfirmation).bind("requireSecurityQuestion");

		questionAdder = new TextField();
		questionAdder.setWidth(TEXT_FIELD_MEDIUM.value());
		SingleActionHandler<String> remove = SingleActionHandler.builder4Delete(msg::getMessage, String.class)
				.withHandler(r -> questions.removeEntry(r.iterator().next())).build();

		questions = new ListOfElementsWithActions<>(
				List.of(new ListOfElementsWithActions.Column<>(null, Span::new, 1)),
				new ListOfElementsWithActions.ActionColumn<>(null, Collections.singletonList(remove), 0, Left));

		addIcon = VaadinIcon.PLUS_CIRCLE_O.create();
		addIcon.addClassNames(POINTER.getName(), FIELD_ICON_GAP.getName());
		addIcon.setTooltipText(msg.getMessage("PasswordCredentialResetSettings.addQuestion"));
		addIcon.addClickListener(e ->
		{
			if(!questionAdder.getValue().isBlank())
			{
				questions.addEntry(questionAdder.getValue());
				questionAdder.setValue("");
			}
			else
				questionAdder.setInvalid(true);
		});
		
	}
	
	public boolean getEmailMessageTemplateState()
	{
		return confirmationMode.getValue().equals(ConfirmationMode.RequireEmail)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailAndMobile)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailOrMobile);
	}

	public boolean getMobileMessageTemplateState()
	{
		return confirmationMode.getValue().equals(ConfirmationMode.RequireMobile)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailAndMobile)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailOrMobile);
	}
	
	private void setEnabled(boolean how)
	{
		codeLength.setEnabled(how);
		confirmationMode.setEnabled(how);
		requireQuestionConfirmation.setEnabled(how);
		emailCodeMessageTemplate.setEnabled(how && getEmailMessageTemplateState());
		mobileCodeMessageTemplate.setEnabled(how && getMobileMessageTemplateState());
		
		if (how)
		{
			boolean state = requireQuestionConfirmation.getValue();
			questionAdder.setEnabled(state);
			questions.setEnabled(state);
			if(!state)
				addIcon.addClassName(DISABLED_ICON.getName());
		} else
		{
			questionAdder.setEnabled(false);
			questions.setEnabled(false);
			addIcon.addClassName(DISABLED_ICON.getName());
		}
			
	}
	
	private void setValue(PasswordCredentialResetSettings initial)
	{
		for (String question: initial.getQuestions())
			questions.addEntry(question);
		binder.setBean(initial);
		setEnabled(initial.isEnabled());
		binder.validate();
	}
	
	public PasswordCredentialResetSettings getValue() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();
		if(questions.getElements().isEmpty() && requireQuestionConfirmation.getValue() && requireQuestionConfirmation.isEnabled())
		{
			questionAdder.setInvalid(true);
			throw new FormValidationException();
		}
		
		PasswordCredentialResetSettings ret = binder.getBean();

		ret.setQuestions(questions.getElements());
		return ret;
	}
	
}
