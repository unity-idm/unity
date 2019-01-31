/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webadmin.reg.invitation;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

public class RegistrationInvitationViewer extends InvitationViewerBase
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InvitationViewer.class);
	
	private RegistrationsManagement regMan;
	private Label expectedIdentity;
	
	public RegistrationInvitationViewer(AttributeHandlerRegistry attrHandlersRegistry,
			MessageTemplateManagement msgTemplateMan, UnityMessageSource msg,
			SharedEndpointManagement sharedEndpointMan, RegistrationsManagement regMan,
			GroupsManagement groupsMan)
	{
		super(attrHandlersRegistry, msgTemplateMan, msg, sharedEndpointMan, groupsMan);
		this.regMan = regMan;
	}

	@Override
	public boolean setInput(InvitationWithCode invitationWithCode)
	{	
		setFormCaption(msg.getMessage("RegistrationInvitationViewer.formId"));
		
		if (super.setInput(invitationWithCode))
		{

			RegistrationInvitationParam regParam = (RegistrationInvitationParam) invitationWithCode
					.getInvitation();
			expectedIdentity.setVisible(regParam.getExpectedIdentity() != null);
			if (regParam.getExpectedIdentity() != null)
				expectedIdentity.setValue(regParam.getExpectedIdentity().toString());

			setLink(PublicRegistrationURLSupport.getPublicRegistrationLink(form,
					invitationWithCode.getRegistrationCode(), sharedEndpointMan));
			return true;
		}
		return false;
	}
	
	@Override
	protected ComponentsContainer getAdditionalFields()
	{
		expectedIdentity = new Label();
		expectedIdentity.setWidth(100, Unit.PERCENTAGE);
		expectedIdentity.setCaption(msg.getMessage("RegistrationInvitationViewer.expectedIdentity"));
		return new ComponentsContainer(expectedIdentity);
	}

	@Override
	protected BaseForm getForm(String id)
	{
		try
		{
			return regMan.getForm(id);
		} catch (EngineException e)
		{
			log.warn("Unable to list registration forms for invitations", e);
			return null;
		}
	}
}
