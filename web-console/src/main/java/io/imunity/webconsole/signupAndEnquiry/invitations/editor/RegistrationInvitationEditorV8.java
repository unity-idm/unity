/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

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
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;

@PrototypeComponent
class RegistrationInvitationEditorV8 extends CustomComponent implements InvitationParamEditor
{
	private enum RemoteIdentityExpectation
	{
		NONE, HINT, REQUIRED
	}

	private final MessageSource msg;
	private final Map<String, BaseForm> formsByName;
	private final Map<String, MessageTemplate> msgTemplates;
	private final PrefillEntryEditorV8 prefillEntryEditor;
	private final MessageParamEditor messageParamEditor;

	private ComboBox<String> forms;
	private DateTimeField expiration;
	private TextField contactAddress;
	private EnumComboBox<RemoteIdentityExpectation> remoteIdentityExpectation;
	private Label channel;

	@Autowired
	RegistrationInvitationEditorV8(MessageSource msg, MessageTemplateManagement messageTemplateManagement,
			RegistrationsManagement registrationManagement, PrefillEntryEditorV8 prefillEntryEditor)
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
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		FormLayoutWithFixedCaptionWidth top = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		main.addComponent(top);

		channel = new Label();
		channel.setCaption(msg.getMessage("InvitationViewer.channelId"));

		forms = new ComboBox<>(msg.getMessage("InvitationEditor.RegistrationFormId"));
		forms.setRequiredIndicatorVisible(true);
		forms.addValueChangeListener(event ->
		{
			BaseForm form = formsByName.get(forms.getValue());
			setPerFormUI(form);
			channel.setValue("");
			if (form == null)
			{
				return;
			}

			String invTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
			if (invTemplate != null && msgTemplates.get(invTemplate) != null)
				channel.setValue(msgTemplates.get(invTemplate).getNotificationChannel());
			else
				channel.setValue("");

		});
		forms.setEmptySelectionAllowed(false);
		forms.setItems(formsByName.keySet());
		if (!formsByName.keySet().isEmpty())
		{
			forms.setSelectedItem(formsByName.keySet().iterator().next());
		} else
		{
			forms.setSelectedItem(null);
		}

		expiration = new DateTimeField(msg.getMessage("InvitationViewer.expiration"));
		expiration.setRequiredIndicatorVisible(true);
		expiration.setResolution(DateTimeResolution.MINUTE);
		expiration.setValue(
				LocalDateTime.now(InvitationEditorV8.DEFAULT_ZONE_ID).plusDays(InvitationEditorV8.DEFAULT_TTL_DAYS));

		contactAddress = new TextField(msg.getMessage("InvitationViewer.contactAddress"));
		contactAddress.setWidth(20, Unit.EM);

		remoteIdentityExpectation = new EnumComboBox<>(msg.getMessage("InvitationEditor.requireSameEmail"), msg,
				"InvitationEditor.idExpectation.", RemoteIdentityExpectation.class, RemoteIdentityExpectation.NONE);

		top.addComponents(forms, channel, expiration, contactAddress, remoteIdentityExpectation, messageParamEditor);

		CollapsibleLayout regLayout = new CollapsibleLayout(msg.getMessage("InvitationEditor.registrationPrefillInfo"),
				prefillEntryEditor);
		regLayout.setMargin(new MarginInfo(false));
		regLayout.expand();
		main.addComponent(regLayout);
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
			forms.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();

		}
		
		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		if (expiration.getValue() == null)
		{
			expiration.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		}

		RegistrationInvitationParam param = new RegistrationInvitationParam(forms.getValue(),
				expiration.getValue().atZone(InvitationEditorV8.DEFAULT_ZONE_ID).toInstant(), addr);
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
	public static class RegistrationInvitationEditorFactoryV8
	{
		private ObjectFactory<RegistrationInvitationEditorV8> editorFactory;

		public RegistrationInvitationEditorFactoryV8(ObjectFactory<RegistrationInvitationEditorV8> editor)
		{
			this.editorFactory = editor;
		}

		public RegistrationInvitationEditorV8 getEditor() throws EngineException
		{
			return editorFactory.getObject();
		}
	}

}
