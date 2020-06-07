/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webadmin.reg.invitations.InvitationEditor;
import io.imunity.webadmin.reg.invitations.InvitationEntry;
import io.imunity.webadmin.reg.invitations.InvitationViewer;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all invitation views
 * 
 * @author P.Piernik
 *
 */
@Component
class InvitationsController
{
	private InvitationManagement invMan;
	private MessageSource msg;
	private RegistrationsManagement registrationManagement;
	private EnquiryManagement enquiryMan;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private MessageTemplateManagement msgTemplateManagement;
	private GroupsManagement groupsManagement;
	private EntityManagement entityManagement;
	private SharedEndpointManagement sharedEndpointManagement;
	private IdentityEditorRegistry identityEditorRegistry;
	private AttributeTypeManagement attributesManagement;
	private NotificationProducer notificationsProducer;
	private BulkGroupQueryService bulkQuery;
	private AttributeSupport attributeSupport;

	@Autowired
	InvitationsController(InvitationManagement invMan, MessageSource msg,
			RegistrationsManagement registrationManagement, EnquiryManagement enquiryMan,
			AttributeHandlerRegistry attrHandlersRegistry, MessageTemplateManagement msgTemplateManagement,
			GroupsManagement groupsManagement, EntityManagement entityManagement,
			SharedEndpointManagement sharedEndpointManagement,
			IdentityEditorRegistry identityEditorRegistry, AttributeTypeManagement attributesManagement,
			NotificationProducer notificationsProducer, BulkGroupQueryService bulkQuery,
			AttributeSupport attributeSupport)
	{
		this.invMan = invMan;
		this.msg = msg;
		this.registrationManagement = registrationManagement;
		this.enquiryMan = enquiryMan;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.msgTemplateManagement = msgTemplateManagement;
		this.groupsManagement = groupsManagement;
		this.entityManagement = entityManagement;
		this.sharedEndpointManagement = sharedEndpointManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.attributesManagement = attributesManagement;
		this.notificationsProducer = notificationsProducer;
		this.bulkQuery = bulkQuery;
		this.attributeSupport = attributeSupport;
	}

	Collection<InvitationEntry> getInvitations() throws ControllerException
	{
		try
		{
			return invMan.getInvitations().stream().map(i -> new InvitationEntry(msg, i))
					.collect(Collectors.toList());
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.getAllError"), e);
		}
	}

	void addInvitation(InvitationParam toAdd) throws ControllerException
	{
		try
		{
			invMan.addInvitation(toAdd);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.addError"), e);
		}
	}

	void sendInvitations(Set<InvitationEntry> items) throws ControllerException
	{
		List<String> sent = new ArrayList<>();
		try
		{
			for (InvitationEntry item : items)
			{
				invMan.sendInvitation(item.getCode());
				sent.add(item.getAddress());
			}
		} catch (Exception e)
		{
			if (sent.isEmpty())
			{
				throw new ControllerException(msg.getMessage("InvitationsController.sendError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("InvitationsController.sendError"),
						msg.getMessage("InvitationsController.partiallySent", sent), e);
			}
		}
	}

	void removeInvitations(Set<InvitationEntry> items) throws ControllerException
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (InvitationEntry item : items)
			{
				invMan.removeInvitation(item.getCode());
				removed.add(item.getAddress());
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				throw new ControllerException(msg.getMessage("InvitationsController.removeError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("InvitationsController.removeError"),
						msg.getMessage("InvitationsController.partiallyRemoved", removed), e);
			}
		}

	}

	InvitationViewer getViewer()
	{
		return new InvitationViewer(msg, attrHandlersRegistry, msgTemplateManagement, registrationManagement,
				enquiryMan, sharedEndpointManagement, entityManagement, groupsManagement);
	}

	InvitationEditor getEditor() throws ControllerException
	{

		try
		{
			return new InvitationEditor(msg, identityEditorRegistry, attrHandlersRegistry,
					msgTemplateManagement.listTemplates(), registrationManagement.getForms(),
					enquiryMan.getEnquires(), attributesManagement.getAttributeTypesAsMap(),
					notificationsProducer, getEntities(), getNameAttribute(),
					groupsManagement.getGroupsByWildcard("/**"), msgTemplateManagement);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.getEditorError"), e);
		}

	}

	InvitationEditor getEditor(String type, String formName) throws ControllerException
	{

		InvitationEditor editor = getEditor();

		try
		{
			InvitationType invType = InvitationType.valueOf(type);
			editor.setType(invType);
			editor.setTypeReadOnly(true);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.unsupportedInvitationType"),
					null);
		}

		try
		{
			editor.setForm(formName);
			editor.setFormsReadOnly(true);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.invalidForm"), e);
		}

		return editor;

	}

	private Map<Long, EntityInGroupData> getEntities() throws EngineException
	{
		GroupMembershipData bulkMembershipData = bulkQuery.getBulkMembershipData("/");
		return bulkQuery.getMembershipInfo(bulkMembershipData);
	}

	private String getNameAttribute() throws EngineException
	{
		AttributeType type = attributeSupport
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		if (type == null)
			return null;
		return type.getName();
	}

}
