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

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;

@PrototypeComponent
class ComboInvitationEditor extends CustomComponent implements InvitationParamEditor
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
	private DateTimeField expiration;
	private TextField contactAddress;
	private Label regChannel;

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
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		FormLayoutWithFixedCaptionWidth top = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		main.addComponent(top);

		regChannel = new Label();
		regChannel.setCaption(msg.getMessage("InvitationViewer.channelId"));

		regForms = new ComboBox<>(msg.getMessage("InvitationEditor.RegistrationFormId"));
		regForms.setRequiredIndicatorVisible(true);
		regForms.addValueChangeListener(event ->
		{
			BaseForm form = regFormsByName.get(regForms.getValue());
			regChannel.setValue("");
			setRegPerFormUI(form);

			String invTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
			if (invTemplate != null && msgTemplates.get(invTemplate) != null)
				regChannel.setValue(msgTemplates.get(invTemplate).getNotificationChannel());
			else
				regChannel.setValue("");

		});
		regForms.setEmptySelectionAllowed(false);
		regForms.setItems(regFormsByName.keySet());

		enqForms = new ComboBox<>(msg.getMessage("InvitationEditor.EnquiryFormId"));

		enqForms.setRequiredIndicatorVisible(true);
		enqForms.addValueChangeListener(event ->
		{
			setEnqPerFormUI(enqFormsByName.get(enqForms.getValue()));
		});
		enqForms.setEmptySelectionAllowed(false);
		enqForms.setItems(enqFormsByName.keySet());

		if (!regFormsByName.keySet().isEmpty())
		{
			regForms.setSelectedItem(regFormsByName.keySet().iterator().next());
		} else
		{
			regForms.setSelectedItem(null);
		}

		if (!enqFormsByName.keySet().isEmpty())
		{
			enqForms.setSelectedItem(enqFormsByName.keySet().iterator().next());
		} else
		{
			enqForms.setSelectedItem(null);
		}

		expiration = new DateTimeField(msg.getMessage("InvitationViewer.expiration"));
		expiration.setRequiredIndicatorVisible(true);
		expiration.setResolution(DateTimeResolution.MINUTE);
		expiration.setValue(
				LocalDateTime.now(InvitationEditor.DEFAULT_ZONE_ID).plusDays(InvitationEditor.DEFAULT_TTL_DAYS));

		contactAddress = new TextField(msg.getMessage("InvitationViewer.contactAddress"));
		contactAddress.setWidth(20, Unit.EM);

		top.addComponents(regForms, regChannel, enqForms, expiration, contactAddress, messageParamEditor);

		CollapsibleLayout regLayout = new CollapsibleLayout(msg.getMessage("InvitationEditor.registrationPrefillInfo"),
				regPrefillEntryEditor);
		regLayout.setMargin(new MarginInfo(false));
		regLayout.expand();

		CollapsibleLayout enqLayout = new CollapsibleLayout(msg.getMessage("InvitationEditor.enquiryPrefillInfo"),
				enqPrefillEntryEditor);
		enqLayout.setMargin(new MarginInfo(false));
		enqLayout.expand();

		main.addComponent(regLayout);
		main.addComponent(enqLayout);

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
			regForms.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		}

		if (enqForms.getValue() == null)
		{
			enqForms.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		}

		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		if (expiration.getValue() == null)
		{
			expiration.setComponentError(new UserError(msg.getMessage("fieldRequired")));
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
