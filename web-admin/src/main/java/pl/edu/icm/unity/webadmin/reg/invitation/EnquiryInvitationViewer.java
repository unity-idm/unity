/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

public class EnquiryInvitationViewer extends InvitationViewerBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationViewer.class);

	private EnquiryManagement enquiryMan;
	private EntityManagement entityMan;
	private Label entity;

	public EnquiryInvitationViewer(AttributeHandlerRegistry attrHandlersRegistry,
			MessageTemplateManagement msgTemplateMan, UnityMessageSource msg,
			SharedEndpointManagement sharedEndpointMan, EnquiryManagement enquiryMan,
			EntityManagement entityMan, GroupsManagement groupMan)
	{
		super(attrHandlersRegistry, msgTemplateMan, msg, sharedEndpointMan, groupMan);
		this.enquiryMan = enquiryMan;
		this.entityMan = entityMan;
	}

	@Override
	public boolean setInput(InvitationWithCode invitationWithCode)
	{
		setFormCaption(msg.getMessage("EnquiryInvitationViewer.formId"));
		
		if (super.setInput(invitationWithCode))
		{
			EnquiryInvitationParam enqParam = (EnquiryInvitationParam) invitationWithCode.getInvitation();
			entity.setVisible(enqParam.getEntity() != null);
			if (enqParam.getEntity() != null)
			{
				entity.setValue(getEntityLabel(enqParam.getEntity()));
			}
			setLink(PublicRegistrationURLSupport.getPublicEnquiryLink(form,
					invitationWithCode.getRegistrationCode(), sharedEndpointMan));
			return true;
		}
		return false;
	}

	private String getEntityLabel(long entity)
	{
		try
		{
			return entityMan.getEntityLabel(new EntityParam(entity)) + " [" + entity +"]";
		} catch (EngineException e)
		{
			log.error("Can not get entity label for " + entity, e);
		}
		return String.valueOf(entity);
	}
	
	@Override
	protected ComponentsContainer getAdditionalFields()
	{
		entity = new Label();
		entity.setWidth(100, Unit.PERCENTAGE);
		entity.setCaption(msg.getMessage("EnquiryInvitationViewer.entity"));
		return new ComponentsContainer(entity);
	}

	@Override
	protected BaseForm getForm(String id)
	{
		List<EnquiryForm> forms;
		try
		{
			forms = enquiryMan.getEnquires();
		} catch (EngineException e)
		{
			log.warn("Unable to list enquiry forms for invitations", e);
			return null;
		}
		Optional<EnquiryForm> found = forms.stream().filter(form -> form.getName().equals(id)).findAny();
		if (found.isPresent())
			return found.get();
		return null;
	}
}
