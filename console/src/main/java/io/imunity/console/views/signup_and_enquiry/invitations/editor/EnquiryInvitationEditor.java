/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
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
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;

@PrototypeComponent
class EnquiryInvitationEditor extends VerticalLayout implements InvitationParamEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationEditor.class);

	private final MessageSource msg;
	private final NotificationProducer notificationProducer;
	private final Map<String, EnquiryForm> formsByName;
	private final Map<String, MessageTemplate> msgTemplates;
	private final PrefillEntryEditor prefillEntryEditor;
	private final MessageParamEditor messageParamEditor;
	private ComboBox<String> forms;
	private DateTimePicker expiration;
	private TextField contactAddress;
	private ComboBox<Long> entity;
	private NativeLabel channel;

	private String entityNameAttr;
	private Map<Long, EntityInGroupData> allEntities;
	private Map<Long, String> availableEntities;

	@Autowired
	EnquiryInvitationEditor(MessageSource msg, MessageTemplateManagement messageTemplateManagement,
			EnquiryManagement enquiryManagement, PrefillEntryEditor prefillEntryEditor,
			NotificationProducer notificationProducer) throws EngineException
	{
		this.msg = msg;
		this.notificationProducer = notificationProducer;
		this.formsByName = enquiryManagement.getEnquires()
				.stream()
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
		setMargin(false);
		setSpacing(false);
		setPadding(false);

		FormLayout top = new FormLayout();
		top.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		top.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		add(top);

		entity = new ComboBox<>();
		entity.setItemLabelGenerator(i -> availableEntities.get(i) + " [" + i + "]");
		entity.addValueChangeListener(e -> reloadContactAddress());
		entity.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		contactAddress = new TextField();
		contactAddress.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

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

			String invTemplate = form.getNotificationsConfiguration()
					.getInvitationTemplate();
			if (invTemplate != null && msgTemplates.get(invTemplate) != null)
				channel.setText(msgTemplates.get(invTemplate)
						.getNotificationChannel());
			else
				channel.setText("");

			reloadEntities();
		});
		forms.setItems(formsByName.keySet());
		if (!formsByName.keySet()
				.isEmpty())
		{
			forms.setValue(formsByName.keySet()
					.iterator()
					.next());
		} else
		{
			forms.setValue(null);
		}
		forms.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		expiration = new DateTimePicker();
		expiration.setValue(LocalDateTime.now(InvitationEditor.DEFAULT_ZONE_ID)
				.plusDays(InvitationEditor.DEFAULT_TTL_DAYS));
		expiration.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());

		top.addFormItem(forms, msg.getMessage("InvitationEditor.EnquiryFormId"));
		top.addFormItem(channel, msg.getMessage("InvitationViewer.channelId"));
		top.addFormItem(expiration, msg.getMessage("InvitationViewer.expiration"));
		top.addFormItem(entity, msg.getMessage("InvitationEditor.entity"));
		top.addFormItem(contactAddress, msg.getMessage("InvitationViewer.contactAddress"));
		top.addFormItem(messageParamEditor, msg.getMessage("InvitationEditor.messageVariables"));

		AccordionPanel enqPanel = new AccordionPanel(msg.getMessage("InvitationEditor.enquiryPrefillInfo"),
				prefillEntryEditor);
		enqPanel.setWidthFull();
		enqPanel.setOpened(true);
		add(enqPanel);
	}

	private void setPerFormUI(BaseForm form)
	{
		prefillEntryEditor.setInput(form);
		messageParamEditor.setMessageParams(form);
	}

	private void reloadEntities()
	{
		availableEntities.clear();
		EnquiryForm form = formsByName.values()
				.stream()
				.filter(f -> f.getName()
						.equals(forms.getValue()))
				.findFirst()
				.orElse(null);
		if (form == null)
		{
			entity.setItems(Collections.emptyList());
			return;
		}

		allEntities.entrySet()
				.stream()
				.filter(e -> e.getValue().relevantEnquiryForms.contains(form.getName()))
				.forEach(e -> availableEntities.put(e.getKey(), getLabel(e.getValue())));

		List<Long> sortedEntities = availableEntities.keySet()
				.stream()
				.sorted()
				.collect(Collectors.toList());
		entity.setItems(sortedEntities);
		entity.setValue(null);
		if (!sortedEntities.isEmpty())
		{
			entity.setValue(sortedEntities.iterator()
					.next());
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

		String invTemplate = form.getNotificationsConfiguration()
				.getInvitationTemplate();
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
			if (name != null && !name.getValues()
					.isEmpty())
			{
				return name.getValues()
						.get(0);
			}
		}

		return "";
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

		EnquiryInvitationParam param = new EnquiryInvitationParam(forms.getValue(), expiration.getValue()
				.atZone(InvitationEditor.DEFAULT_ZONE_ID)
				.toInstant(), addr);
		if (entity.getValue() == null)
		{
			entity.setErrorMessage(msg.getMessage("fieldRequired"));
			throw new FormValidationException();
		}

		param.setEntity(entity.getValue());

		prefillEntryEditor.prefill(param.getFormPrefill());
		param.getFormPrefill()
				.setMessageParams(messageParamEditor.getParams());

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

		public EnquiryInvitationEditor getEditor(String entityNameAttr, Map<Long, EntityInGroupData> allEntities)
				throws EngineException
		{
			return editorFactory.getObject()
					.init(entityNameAttr, allEntities);
		}
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
