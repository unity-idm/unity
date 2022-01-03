/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.signupAndEnquiry.invitations.editor.EnquiryInvitationEditor.EnquiryInvitationEditorFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Edit UI for {@link InvitationParam}.
 * 
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class InvitationEditor extends CustomComponent
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

		FormLayout top = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		top.setMargin(new MarginInfo(false, true));

		type = new ComboBox<>(msg.getMessage("InvitationEditor.type"));
		type.setItemCaptionGenerator(i -> msg.getMessage("InvitationType." + i.toString().toLowerCase()));
		type.setItems(InvitationType.values());
		type.setEmptySelectionAllowed(false);
		type.addValueChangeListener(e ->
		{
			content.removeAllComponents();
			switchEditor(type.getValue(), content);
		});
		top.addComponent(type);
		
		inviter = new ComboBox<>(msg.getMessage("InvitationEditor.inviter"));
		inviter.setEmptySelectionAllowed(false);
		inviter.setItemCaptionGenerator(i -> getLabel(allEntities.get(i)) + " [" + i + "]");
		inviter.setWidth(20, Unit.EM);
		inviter.setItems(allEntities.keySet());
		inviter.setValue(InvocationContext.getCurrent().getLoginSession().getEntityId());
		
		top.addComponent(inviter);
		
		VerticalLayout main = new VerticalLayout(top, content);
		main.setSpacing(false);
		main.setMargin(false);
		setCompositionRoot(main);

		type.setSelectedItem(InvitationType.REGISTRATION);
	}

	private void switchEditor(InvitationType type, Layout content)
	{
		if (type.equals(InvitationType.REGISTRATION))
		{
			editor = registrationInvitationEditor;
			content.addComponent(registrationInvitationEditor);
		} else if (type.equals(InvitationType.ENQUIRY))
		{
			editor = enquiryInvitationEditor;
			content.addComponent(enquiryInvitationEditor);
		} else if (type.equals(InvitationType.COMBO))
		{
			editor = comboInvitationEditor;
			content.addComponent(comboInvitationEditor);
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
			if (name != null && !name.getValues().isEmpty())
			{
				return name.getValues().get(0);
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
