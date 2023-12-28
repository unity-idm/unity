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
import io.imunity.vaadin.elements.EnumComboBox;
import pl.edu.icm.unity.base.authn.ExpectedIdentity;
import pl.edu.icm.unity.base.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;

@PrototypeComponent
class RegistrationInvitationEditor extends VerticalLayout implements InvitationParamEditor
{
	private enum RemoteIdentityExpectation
	{
		NONE, HINT, REQUIRED
	}

	private final MessageSource msg;
	private final Map<String, BaseForm> formsByName;
	private final Map<String, MessageTemplate> msgTemplates;
	private final PrefillEntryEditor prefillEntryEditor;
	private final MessageParamEditor messageParamEditor;

	private ComboBox<String> forms;
	private DateTimePicker expiration;
	private TextField contactAddress;
	private EnumComboBox<RemoteIdentityExpectation> remoteIdentityExpectation;
	private NativeLabel channel;

	@Autowired
	RegistrationInvitationEditor(MessageSource msg, MessageTemplateManagement messageTemplateManagement,
			RegistrationsManagement registrationManagement, PrefillEntryEditor prefillEntryEditor)
			throws EngineException
	{
		this.msg = msg;
		this.formsByName = registrationManagement.getForms().stream()
				.filter(form -> form.getRegistrationCode() == null && form.isPubliclyAvailable())
				.collect(Collectors.toMap(RegistrationForm::getName, form -> form));
		this.msgTemplates = messageTemplateManagement.listTemplates();
		this.prefillEntryEditor = prefillEntryEditor;
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

		channel = new NativeLabel();
		forms = new ComboBox<>();
		forms.setRequiredIndicatorVisible(true);
		forms.addValueChangeListener(event ->
		{
			BaseForm form = formsByName.get(forms.getValue());
			setPerFormUI(form);
			channel.setText("");
			if (form == null)
			{
				return;
			}

			String invTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
			if (invTemplate != null && msgTemplates.get(invTemplate) != null)
				channel.setText(msgTemplates.get(invTemplate).getNotificationChannel());
			else
				channel.setText("");

		});
		forms.setItems(formsByName.keySet());
		if (!formsByName.keySet().isEmpty())
		{
			forms.setValue(formsByName.keySet().iterator().next());
		} else
		{
			forms.setValue(null);
		}
		forms.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		expiration = new DateTimePicker();
		expiration.setValue(LocalDateTime.now(InvitationEditor.DEFAULT_ZONE_ID).plusDays(InvitationEditor.DEFAULT_TTL_DAYS));
		expiration.setRequiredIndicatorVisible(true);
		expiration.setValue(
				LocalDateTime.now(InvitationEditor.DEFAULT_ZONE_ID).plusDays(InvitationEditor.DEFAULT_TTL_DAYS));
		contactAddress = new TextField();
		contactAddress.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		remoteIdentityExpectation = new EnumComboBox<>(msg::getMessage,
				"InvitationEditor.idExpectation.", RemoteIdentityExpectation.class, RemoteIdentityExpectation.NONE);
		remoteIdentityExpectation.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		top.addFormItem(forms, msg.getMessage("InvitationEditor.RegistrationFormId"));
		top.addFormItem(channel, msg.getMessage("InvitationViewer.channelId"));
		top.addFormItem(expiration, msg.getMessage("InvitationViewer.expiration"));
		top.addFormItem(contactAddress, msg.getMessage("InvitationViewer.contactAddress"));
		top.addFormItem(remoteIdentityExpectation, msg.getMessage("InvitationEditor.requireSameEmail"));
		top.addFormItem(messageParamEditor, msg.getMessage("InvitationEditor.messageVariables"));	
		
		AccordionPanel accordionPanel = new AccordionPanel(msg.getMessage("InvitationEditor.registrationPrefillInfo"),
				prefillEntryEditor);
		accordionPanel.setWidthFull();
		accordionPanel.setOpened(true);
		add(accordionPanel);
	}

	private void setPerFormUI(BaseForm form)
	{
		prefillEntryEditor.setInput(form);
		messageParamEditor.setMessageParams(form);
	}

	@Override
	public InvitationParam getInvitation() throws FormValidationException
	{

		if (forms.getValue() == null)
		{
			forms.setErrorMessage(msg.getMessage("fieldRequired"));
			throw new FormValidationException();

		}
		
		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		if (expiration.getValue() == null)
		{
			expiration.setErrorMessage(msg.getMessage("fieldRequired"));
			throw new FormValidationException();
		}

		RegistrationInvitationParam param = new RegistrationInvitationParam(forms.getValue(),
				expiration.getValue().atZone(InvitationEditor.DEFAULT_ZONE_ID).toInstant(), addr);
		if (addr != null && remoteIdentityExpectation.getValue() != RemoteIdentityExpectation.NONE)
		{
			IdentityExpectation expectation = remoteIdentityExpectation.getValue() == RemoteIdentityExpectation.HINT
					? IdentityExpectation.HINT
					: IdentityExpectation.MANDATORY;
			param.setExpectedIdentity(new ExpectedIdentity(addr, expectation));
		}

		prefillEntryEditor.prefill(param.getFormPrefill());
		param.getFormPrefill().setMessageParams(messageParamEditor.getParams());

		return param;
	}

	public void setForm(String form)
	{
		forms.setValue(form);
		forms.setReadOnly(true);
	}

	@org.springframework.stereotype.Component
	public static class RegistrationInvitationEditorFactory
	{
		private ObjectFactory<RegistrationInvitationEditor> editorFactory;

		public RegistrationInvitationEditorFactory(ObjectFactory<RegistrationInvitationEditor> editor)
		{
			this.editorFactory = editor;
		}

		public RegistrationInvitationEditor getEditor() throws EngineException
		{
			return editorFactory.getObject();
		}
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

}
