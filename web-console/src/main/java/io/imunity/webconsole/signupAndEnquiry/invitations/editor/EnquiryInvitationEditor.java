/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
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

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invite.InvitationParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;

@PrototypeComponent
class EnquiryInvitationEditor extends CustomComponent implements InvitationParamEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationEditor.class);

	private final MessageSource msg;
	private final NotificationProducer notificationProducer;
	private final Map<String, EnquiryForm> formsByName;
	private final Map<String, MessageTemplate> msgTemplates;
	private final PrefillEntryEditor prefillEntryEditor;
	private final MessageParamEditor messageParamEditor;
	private ComboBox<String> forms;
	private DateTimeField expiration;
	private TextField contactAddress;
	private ComboBox<Long> entity;
	private Label channel;

	private  String entityNameAttr;
	private  Map<Long, EntityInGroupData> allEntities;
	private  Map<Long, String> availableEntities;
	
	@Autowired
	EnquiryInvitationEditor(MessageSource msg, MessageTemplateManagement messageTemplateManagement,
			EnquiryManagement enquiryManagement, 
			PrefillEntryEditor prefillEntryEditor, NotificationProducer notificationProducer) throws EngineException
	{
		this.msg = msg;
		this.notificationProducer = notificationProducer;
		this.formsByName = enquiryManagement.getEnquires().stream()
				.collect(Collectors.toMap(EnquiryForm::getName, form -> form));
		this.msgTemplates = messageTemplateManagement.listTemplates();
		this.availableEntities = new HashMap<>();
		this.prefillEntryEditor = prefillEntryEditor;
		this.messageParamEditor = new MessageParamEditor(msg, msgTemplates);
	}

	public EnquiryInvitationEditor init(String entityNameAttr, Map<Long, EntityInGroupData> allEntities)
	{
		this.entityNameAttr = entityNameAttr;
		this.allEntities = allEntities;
		initUI();
		return this;
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		FormLayoutWithFixedCaptionWidth top = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		main.addComponent(top);

		entity = new ComboBox<>(msg.getMessage("InvitationEditor.entity"));
		entity.setEmptySelectionAllowed(false);
		entity.setItemCaptionGenerator(i -> availableEntities.get(i) + " [" + i + "]");
		entity.setWidth(20, Unit.EM);
		entity.addSelectionListener(e -> reloadContactAddress());

		contactAddress = new TextField(msg.getMessage("InvitationViewer.contactAddress"));
		contactAddress.setWidth(20, Unit.EM);

		channel = new Label();
		channel.setCaption(msg.getMessage("InvitationViewer.channelId"));

		forms = new ComboBox<>(msg.getMessage("InvitationEditor.EnquiryFormId"));

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

			reloadEntities();
		});
		forms.setEmptySelectionAllowed(false);
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
				LocalDateTime.now(InvitationEditor.DEFAULT_ZONE_ID).plusDays(InvitationEditor.DEFAULT_TTL_DAYS));

		top.addComponents(forms, channel, expiration, entity, contactAddress, messageParamEditor);

		CollapsibleLayout enqLayout = new CollapsibleLayout(msg.getMessage("InvitationEditor.enquiryPrefillInfo"),
				prefillEntryEditor);
		enqLayout.setMargin(new MarginInfo(false));
		enqLayout.expand();
		main.addComponent(enqLayout);

	}

	private void setPerFormUI(BaseForm form)
	{
		prefillEntryEditor.setInput(form);
		messageParamEditor.setMessageParams(form);
	}

	private void reloadEntities()
	{
		availableEntities.clear();
		EnquiryForm form = formsByName.values().stream().filter(f -> f.getName().equals(forms.getValue())).findFirst()
				.orElse(null);
		if (form == null)
		{
			entity.setItems(Collections.emptyList());
			return;
		}

		allEntities.entrySet().stream().filter(e -> e.getValue().relevantEnquiryForms.contains(form.getName()))
				.forEach(e -> availableEntities.put(e.getKey(), getLabel(e.getValue())));

		List<Long> sortedEntities = availableEntities.keySet().stream().sorted().collect(Collectors.toList());
		entity.setItems(sortedEntities);
		entity.setSelectedItem(null);
		if (!sortedEntities.isEmpty())
		{
			entity.setSelectedItem(sortedEntities.iterator().next());
		}
	}

	private void reloadContactAddress()
	{
		Long entityVal = entity.getValue();
		contactAddress.clear();
		if (entityVal == null)
			return;
		BaseForm form = formsByName.get(forms.getValue());

		if (form == null)
		{
			return;
		}

		String invTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
		if (invTemplate == null)
			return;

		try
		{
			contactAddress
					.setValue(notificationProducer.getAddressForEntity(new EntityParam(entityVal), invTemplate, false));
		} catch (EngineException e1)
		{
			log.error("Can not get address for entity " + entityVal);
		}
	}

	String getLabel(EntityInGroupData info)
	{
		if (entityNameAttr != null)
		{
			AttributeExt name = info.rootAttributesByName.get(entityNameAttr);
			if (name != null && !name.getValues().isEmpty())
			{
				return name.getValues().get(0);
			}
		}

		return "";
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

		EnquiryInvitationParam param = new EnquiryInvitationParam(forms.getValue(),
				expiration.getValue().atZone(InvitationEditor.DEFAULT_ZONE_ID).toInstant(), addr);
		if (entity.getValue() == null)
		{
			entity.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		}

		param.setEntity(entity.getValue());

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
	public static class EnquiryInvitationEditorFactory
	{
		private ObjectFactory<EnquiryInvitationEditor> editorFactory;

		public EnquiryInvitationEditorFactory(ObjectFactory<EnquiryInvitationEditor> editor)
		{
			this.editorFactory = editor;
		}

		public EnquiryInvitationEditor getEditor(String entityNameAttr, Map<Long, EntityInGroupData> allEntities) throws EngineException
		{
			return editorFactory.getObject().init(entityNameAttr, allEntities);
		}
	}
}
