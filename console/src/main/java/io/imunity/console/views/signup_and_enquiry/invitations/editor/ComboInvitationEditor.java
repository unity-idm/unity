/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import io.imunity.vaadin.elements.CSSVars;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;

@PrototypeComponent
class ComboInvitationEditor extends VerticalLayout implements InvitationParamEditor
{
	private final MessageSource msg;
	private final Map<String, BaseForm> regFormsByName;
	private final Map<String, BaseForm> enqFormsByName;
	private final Map<String, MessageTemplate> msgTemplates;
	private final PrefillEntryEditor regPrefillEntryEditor;
	private final PrefillEntryEditor enqPrefillEntryEditor;
	private final MessageParamEditor messageParamEditor;

	private ComboBox<String> regForms;
	private ComboBox<String> enqForms;
	private DateTimePicker expiration;
	private TextField contactAddress;
	private NativeLabel regChannel;

	@Autowired
	ComboInvitationEditor(MessageSource msg, MessageTemplateManagement messageTemplateManagement,
			RegistrationsManagement registrationsManagement, EnquiryManagement enquiryManagement,
			PrefillEntryEditor regPrefillEntryEditor, PrefillEntryEditor enqPrefillEntryEditor) throws EngineException
	{
		this.msg = msg;
		this.regFormsByName = registrationsManagement.getForms().stream()
				.filter(form -> form.getRegistrationCode() == null && form.isPubliclyAvailable())
				.collect(Collectors.toMap(RegistrationForm::getName, form -> form));
		this.enqFormsByName = enquiryManagement.getEnquires().stream()
				.collect(Collectors.toMap(EnquiryForm::getName, form -> form));
		this.msgTemplates = messageTemplateManagement.listTemplates();
		this.regPrefillEntryEditor = regPrefillEntryEditor;
		this.enqPrefillEntryEditor = enqPrefillEntryEditor;
		this.messageParamEditor = new MessageParamEditor(msg, msgTemplates);

		initUI();
	}

	private void initUI()
	{
		setMargin(false);
		setSpacing(false);
		setPadding(false);

		FormLayout top = new FormLayout();
		top.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		top.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		add(top);
		regChannel = new NativeLabel();

		regForms = new ComboBox<>();
		regForms.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		regForms.setRequiredIndicatorVisible(true);
		regForms.addValueChangeListener(event ->
		{
			BaseForm form = regFormsByName.get(regForms.getValue());
			regChannel.setText("");
			setRegPerFormUI(form);

			String invTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
			if (invTemplate != null && msgTemplates.get(invTemplate) != null)
				regChannel.setText(msgTemplates.get(invTemplate).getNotificationChannel());
			else
				regChannel.setText("");

		});
		regForms.setItems(regFormsByName.keySet());

		enqForms = new ComboBox<>();
		enqForms.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		enqForms.setRequiredIndicatorVisible(true);
		enqForms.addValueChangeListener(event ->
		{
			setEnqPerFormUI(enqFormsByName.get(enqForms.getValue()));
		});
		enqForms.setItems(enqFormsByName.keySet());

		if (!regFormsByName.keySet().isEmpty())
		{
			regForms.setValue(regFormsByName.keySet().iterator().next());
		} else
		{
			regForms.setValue(null);
		}

		if (!enqFormsByName.keySet().isEmpty())
		{
			enqForms.setValue(enqFormsByName.keySet().iterator().next());
		} else
		{
			enqForms.setValue(null);
		}

		expiration = new DateTimePicker();
		expiration.setValue(LocalDateTime.now(InvitationEditor.DEFAULT_ZONE_ID).plusDays(InvitationEditor.DEFAULT_TTL_DAYS));
		expiration.setRequiredIndicatorVisible(true);
		expiration.setValue(
				LocalDateTime.now(InvitationEditor.DEFAULT_ZONE_ID).plusDays(InvitationEditor.DEFAULT_TTL_DAYS));
		expiration.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		contactAddress = new TextField();
		contactAddress.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		top.addFormItem(regForms, msg.getMessage("InvitationEditor.RegistrationFormId"));
		top.addFormItem(regChannel, msg.getMessage("InvitationViewer.channelId"));
		top.addFormItem(enqForms, msg.getMessage("InvitationEditor.EnquiryFormId"));
		top.addFormItem(expiration, msg.getMessage("InvitationViewer.expiration"));
		top.addFormItem(contactAddress, msg.getMessage("InvitationViewer.contactAddress"));
		top.addFormItem(messageParamEditor, msg.getMessage("InvitationEditor.messageVariables"));	

		AccordionPanel regPanel = new AccordionPanel(msg.getMessage("InvitationEditor.registrationPrefillInfo"),
				regPrefillEntryEditor);
		regPanel.setWidthFull();
		regPanel.setOpened(true);
		add(regPanel);
		
		AccordionPanel enqPanel = new AccordionPanel(msg.getMessage("InvitationEditor.enquiryPrefillInfo"),
				enqPrefillEntryEditor);
	//	enqPanel.setWidthFull();
		enqPanel.setOpened(true);
		add(enqPanel);
	}

	private void setRegPerFormUI(BaseForm regForm)
	{
		regPrefillEntryEditor.setInput(regForm);
		messageParamEditor.setMessageParams(regForm);
		setCommonMessageParams();
	}

	private void setEnqPerFormUI(BaseForm form)
	{
		enqPrefillEntryEditor.setInput(form);
		setCommonMessageParams();
	}

	private void setCommonMessageParams()
	{
		BaseForm regForm = regForms.getValue() == null ? null : regFormsByName.get(regForms.getValue());
		BaseForm enqForm = enqForms.getValue() == null ? null : enqFormsByName.get(enqForms.getValue());
		messageParamEditor.setMessageParams(regForm, enqForm);
	}

	@Override
	public InvitationParam getInvitation() throws FormValidationException
	{

		if (regForms.getValue() == null)
		{
			regForms.setErrorMessage(msg.getMessage("fieldRequired"));
			throw new FormValidationException();
		}

		if (enqForms.getValue() == null)
		{
			enqForms.setErrorMessage(msg.getMessage("fieldRequired"));
			throw new FormValidationException();
		}

		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		if (expiration.getValue() == null)
		{
			expiration.setErrorMessage(msg.getMessage("fieldRequired"));
			throw new FormValidationException();
		}

		ComboInvitationParam param = new ComboInvitationParam(regForms.getValue(), enqForms.getValue(),
				expiration.getValue().atZone(InvitationEditor.DEFAULT_ZONE_ID).toInstant(), addr);
		regPrefillEntryEditor.prefill(param.getRegistrationFormPrefill());
		enqPrefillEntryEditor.prefill(param.getEnquiryFormPrefill());

		param.getRegistrationFormPrefill().setMessageParams(messageParamEditor.getParams());
		param.getEnquiryFormPrefill().setMessageParams(messageParamEditor.getParams());

		return param;
	}
	
	@Override
	public Component getComponent()
	{
		return this; 
	}

	@org.springframework.stereotype.Component
	public static class ComboInvitationEditorFactory
	{
		private ObjectFactory<ComboInvitationEditor> editorFactory;

		public ComboInvitationEditorFactory(ObjectFactory<ComboInvitationEditor> editor)
		{
			this.editorFactory = editor;
		}

		public ComboInvitationEditor getEditor() throws EngineException
		{
			return editorFactory.getObject();
		}
	}

	

}
