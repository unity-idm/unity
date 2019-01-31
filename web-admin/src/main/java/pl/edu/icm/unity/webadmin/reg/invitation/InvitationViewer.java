/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Presents an {@link InvitationWithCode}
 * 
 * @author Krzysztof Benedyczak
 */
public class InvitationViewer extends CustomComponent
{
	private RegistrationInvitationViewer regViewer;
	private EnquiryInvitationViewer enqViewer;

	public InvitationViewer(UnityMessageSource msg, AttributeHandlerRegistry attrHandlersRegistry,
			MessageTemplateManagement msgTemplateMan, RegistrationsManagement regMan,
			EnquiryManagement enquiryMan, SharedEndpointManagement sharedEndpointMan,
			EntityManagement entityMan, GroupsManagement groupMan)
	{
		this.regViewer = new RegistrationInvitationViewer(attrHandlersRegistry, msgTemplateMan, msg,
				sharedEndpointMan, regMan, groupMan);
		this.enqViewer = new EnquiryInvitationViewer(attrHandlersRegistry, msgTemplateMan, msg,
				sharedEndpointMan, enquiryMan, entityMan, groupMan);
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);
		main.addComponents(regViewer, enqViewer);
	}

	public void setInput(InvitationWithCode invitationWithCode)
	{
		enqViewer.setInput(null);
		regViewer.setInput(null);

		if (invitationWithCode == null)
		{
			return;
		}

		InvitationParam invitation = invitationWithCode.getInvitation();
		InvitationType itype = invitation.getType();

		if (itype.equals(InvitationType.REGISTRATION))
		{
			regViewer.setInput(invitationWithCode);

		} else
		{
			enqViewer.setInput(invitationWithCode);
		}
	}
}
