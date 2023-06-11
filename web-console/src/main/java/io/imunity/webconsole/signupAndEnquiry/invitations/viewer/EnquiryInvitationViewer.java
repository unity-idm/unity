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

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

@PrototypeComponent
class EnquiryInvitationViewer extends CustomComponent implements InvitationViewer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationViewer.class);

	private final MessageSource msg;
	private final EntityManagement entityMan;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final CommonInvitationFieldViewer baseViewer;
	private final PrefilledEntriesViewer prefillViewer;
	private final ViewerUtils utils;

	private Label entity;
	private Label formId;

	EnquiryInvitationViewer(PublicRegistrationURLSupport publicRegistrationURLSupport, MessageSource msg,
			CommonInvitationFieldViewer baseViewer, PrefilledEntriesViewer prefillViewer,
			EntityManagement entityManagement, ViewerUtils utils)
	{
		this.msg = msg;
		this.entityMan = entityManagement;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
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

	public void setInput(InvitationWithCode invitationWithCode) throws IllegalFormTypeException
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
				invitationWithCode, utils.getChannel(form), publicRegistrationURLSupport
						.getPublicFormLink(form.getName(), FormType.ENQUIRY, invitationWithCode.getRegistrationCode()),
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
