/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

@PrototypeComponent
class EnquiryInvitationViewer extends CustomComponent implements InvitationViewer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationViewer.class);

	private final MessageSource msg;
	private final EntityManagement entityMan;
	private final SharedEndpointManagement sharedEndpointManagement;
	private final CommonInvitationFieldViewer baseViewer;
	private final PrefilledEntriesViewer prefillViewer;
	private final ViewerUtils utils;

	private Label entity;
	private Label formId;

	EnquiryInvitationViewer(SharedEndpointManagement sharedEndpointMan, MessageSource msg,
			CommonInvitationFieldViewer baseViewer, PrefilledEntriesViewer prefillViewer,
			EntityManagement entityManagement, ViewerUtils utils)
	{
		this.msg = msg;
		this.entityMan = entityManagement;
		this.sharedEndpointManagement = sharedEndpointMan;
		this.baseViewer = baseViewer;
		this.prefillViewer = prefillViewer;
		this.utils = utils;
		init();
	}

	private void init()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(new MarginInfo(false, true));
		setCompositionRoot(main);
		FormLayoutWithFixedCaptionWidth top = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		top.setMargin(false);

		formId = new Label();
		formId.setCaption(msg.getMessage("EnquiryInvitationViewer.formId"));

		entity = new Label();
		entity.setWidth(100, Unit.PERCENTAGE);
		entity.setCaption(msg.getMessage("EnquiryInvitationViewer.entity"));

		top.addComponents(formId, entity);
		main.addComponent(top);
		main.addComponent(baseViewer);
		FormLayoutWithFixedCaptionWidth prefill = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		prefill.setMargin(false);
		prefillViewer.setCaption(msg.getMessage("EnquiryInvitationViewer.enquiryPrefillInfo"));
		prefill.addComponents(prefillViewer);
		main.addComponent(prefill);
	}

	public void setInput(InvitationWithCode invitationWithCode)
	{
		if (invitationWithCode == null)
		{
			setVisible(false);
			return;
		}

		EnquiryInvitationParam enqParam = (EnquiryInvitationParam) invitationWithCode.getInvitation();
		entity.setVisible(enqParam.getEntity() != null);
		if (enqParam.getEntity() != null)
		{
			entity.setValue(getEntityLabel(enqParam.getEntity()));
		}
		BaseForm form = utils.getEnquiryForm(enqParam.getFormPrefill().getFormId());
		if (form == null)
		{
			return;
		}
		formId.setValue(form.getName());
		prefillViewer.setInput(form, enqParam.getFormPrefill());
		baseViewer.setInput(
				invitationWithCode, utils.getChannel(form), PublicRegistrationURLSupport.getPublicRegistrationLink(form,
						invitationWithCode.getRegistrationCode(), sharedEndpointManagement),
				enqParam.getFormPrefill().getMessageParams());
	}

	private String getEntityLabel(long entity)
	{
		try
		{
			return entityMan.getEntityLabel(new EntityParam(entity)) + " [" + entity + "]";
		} catch (EngineException e)
		{
			log.error("Can not get entity label for " + entity, e);
		}
		return String.valueOf(entity);
	}

	@Override
	public InvitationType getSupportedType()
	{
		return InvitationType.ENQUIRY;
	}
}
