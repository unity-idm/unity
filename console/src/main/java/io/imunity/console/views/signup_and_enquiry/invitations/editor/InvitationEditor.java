/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.console.views.signup_and_enquiry.invitations.editor.EnquiryInvitationEditor.EnquiryInvitationEditorFactory;
import io.imunity.vaadin.elements.CSSVars;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Edit UI for {@link InvitationParam}.
 * 
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class InvitationEditor extends VerticalLayout
{
	public static final long DEFAULT_TTL_DAYS = 3;
	public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
	private final MessageSource msg;
	private final RegistrationInvitationEditor registrationInvitationEditor;
	private final EnquiryInvitationEditor enquiryInvitationEditor;
	private final ComboInvitationEditor comboInvitationEditor;
	private final Map<Long, EntityInGroupData> allEntities;

	private ComboBox<InvitationType> type;
	private InvitationParamEditor editor;
	private ComboBox<Long> inviter;
	private String entityNameAttr;

	public InvitationEditor(MessageSource msg, RegistrationInvitationEditor registrationInvitationEditor,
			EnquiryInvitationEditorFactory enquiryInvitationEditorFactory, ComboInvitationEditor comboInvitationEditor,
			BulkGroupQueryService bulkGroupQueryService, AttributeSupport attributeSupport) throws EngineException
	{
		this.msg = msg;
		this.registrationInvitationEditor = registrationInvitationEditor;
		this.allEntities = getEntities(bulkGroupQueryService);
		this.entityNameAttr = getNameAttribute(attributeSupport);
		this.enquiryInvitationEditor = enquiryInvitationEditorFactory.getEditor(entityNameAttr, allEntities);
		this.comboInvitationEditor = comboInvitationEditor;
		initUI();
	}

	private void initUI()
	{
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setMargin(false);
		content.setSpacing(false);
		content.setPadding(false);

		FormLayout top = new FormLayout();
		top.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		top.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		type = new ComboBox<>();
		type.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		type.setItemLabelGenerator(i -> msg.getMessage("InvitationType." + i.toString()
				.toLowerCase()));

		type.setItems(InvitationType.values());
		type.addValueChangeListener(e ->
		{
			content.removeAll();
			switchEditor(type.getValue(), content);
		});
		top.addFormItem(type, msg.getMessage("InvitationEditor.type"));

		inviter = new ComboBox<>();
		inviter.setItemLabelGenerator(i -> getLabel(allEntities.get(i)) + " [" + i + "]");
		inviter.setItems(allEntities.keySet());
		inviter.setValue(InvocationContext.getCurrent()
				.getLoginSession()
				.getEntityId());
		inviter.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		
		top.addFormItem(inviter, msg.getMessage("InvitationEditor.inviter"));
		removeAll();
		add(top, content);
		setSpacing(false);
		
		type.setValue(InvitationType.REGISTRATION);
	}

	private void switchEditor(InvitationType type, VerticalLayout content)
	{
		if (type.equals(InvitationType.REGISTRATION))
		{
			editor = registrationInvitationEditor;
			content.add(registrationInvitationEditor.getComponent());
		} else if (type.equals(InvitationType.ENQUIRY))
		{
			editor = enquiryInvitationEditor;
			content.add(enquiryInvitationEditor.getComponent());
		} else if (type.equals(InvitationType.COMBO))
		{
			editor = comboInvitationEditor;
			content.add(comboInvitationEditor.getComponent());
		}
	}

	public InvitationParam getInvitation() throws FormValidationException
	{
		InvitationParam invitation = editor.getInvitation();
		invitation.setInviterEntity(inviter.getOptionalValue());
		return invitation;
	}

	public void setInvitationToForm(InvitationType type, String form)
	{
		if (type == InvitationType.ENQUIRY)
		{
			setFixedEnquiryForm(form);
		} else if (type == InvitationType.REGISTRATION)
		{
			setFixedRegistrationForm(form);
		} else
		{
			throw new UnsupportedOperationException("Can not preset combo invitation");
		}
	}

	private void setFixedEnquiryForm(String form)
	{
		type.setValue(InvitationType.ENQUIRY);
		type.setReadOnly(true);
		enquiryInvitationEditor.setForm(form);
	}

	private void setFixedRegistrationForm(String form)
	{
		type.setValue(InvitationType.REGISTRATION);
		type.setReadOnly(true);
		registrationInvitationEditor.setForm(form);
	}

	private String getNameAttribute(AttributeSupport attributeSupport) throws EngineException
	{
		AttributeType type = attributeSupport.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		if (type == null)
			return null;
		return type.getName();
	}

	private Map<Long, EntityInGroupData> getEntities(BulkGroupQueryService bulkQuery) throws EngineException
	{
		GroupMembershipData bulkMembershipData = bulkQuery.getBulkMembershipData("/");
		return bulkQuery.getMembershipInfo(bulkMembershipData);
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

	@org.springframework.stereotype.Component
	public static class InvitationEditorFactory
	{
		private ObjectFactory<InvitationEditor> editorFactory;

		public InvitationEditorFactory(ObjectFactory<InvitationEditor> editor)
		{
			this.editorFactory = editor;
		}

		public InvitationEditor getEditor() throws EngineException
		{
			return editorFactory.getObject();
		}
	}
}
