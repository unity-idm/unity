/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Management of registration invitations.
 * @author Krzysztof Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InvitationsComponent extends CustomComponent implements ActivationListener
{
	private UnityMessageSource msg;
	private RegistrationsManagement registrationManagement;
	private EnquiryManagement enquiryMan;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private IdentityEditorRegistry identityEditorRegistry;
	private MessageTemplateManagement msgTemplateManagement;
	private AttributeSupport attributeSupport;

	private AttributeTypeManagement attributesManagement;
	private InvitationManagement invitationManagement;
	private GroupsManagement groupsManagement;
	private EntityManagement entityManagement;
	private BulkGroupQueryService bulkQuery;
	private NotificationProducer notificationProducer;
	
	private SharedEndpointManagement sharedEndpointManagement;

	private InvitationsTable invitationsTable;
	
	@Autowired
	public InvitationsComponent(UnityMessageSource msg,
			RegistrationsManagement registrationManagement,
			EnquiryManagement enquiryMan,
			AttributeTypeManagement attributesManagement,
			InvitationManagement invitationManagement,
			AttributeHandlerRegistry attrHandlersRegistry,
			IdentityEditorRegistry identityEditorRegistry,
			MessageTemplateManagement msgTemplateManagement,
			GroupsManagement groupsManagement,
			EntityManagement entityManagement,
			BulkGroupQueryService bulkQuery,
			NotificationProducer notificationProducer,
			SharedEndpointManagement sharedEndpointManagement,
			AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.registrationManagement = registrationManagement;
		this.enquiryMan = enquiryMan;
		this.attributesManagement = attributesManagement;
		this.invitationManagement = invitationManagement;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.identityEditorRegistry = identityEditorRegistry;
		this.msgTemplateManagement = msgTemplateManagement;
		this.groupsManagement = groupsManagement;
		this.entityManagement = entityManagement;
		this.bulkQuery = bulkQuery;
		this.notificationProducer = notificationProducer;
		this.sharedEndpointManagement = sharedEndpointManagement;
		this.attributeSupport = attributeSupport;
		initUI();
	}

	private void initUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		invitationsTable = new InvitationsTable(msg,
				registrationManagement, enquiryMan, invitationManagement, attributesManagement,
				identityEditorRegistry, attrHandlersRegistry,
				msgTemplateManagement, groupsManagement, notificationProducer, bulkQuery, attributeSupport);
		InvitationViewer viewer = new InvitationViewer(msg, attrHandlersRegistry,
				msgTemplateManagement, registrationManagement, enquiryMan, sharedEndpointManagement, 
				entityManagement, groupsManagement);

		invitationsTable.addValueChangeListener(invitation -> 
			viewer.setInput(invitation)
		);
		
		CompositeSplitPanel hl = new CompositeSplitPanel(false, true, invitationsTable, viewer, 40);
		setCompositionRoot(hl);
		setCaption(msg.getMessage("InvitationsComponent.caption"));
	}
	
	@Override
	public void stateChanged(boolean enabled)
	{
		if (enabled)
			invitationsTable.refresh();
	}
}
