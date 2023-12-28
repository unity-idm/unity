/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.viewer;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
class EnquiryInvitationViewer extends VerticalLayout implements InvitationViewer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationViewer.class);

	private final MessageSource msg;
	private final EntityManagement entityMan;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final CommonInvitationFieldViewer baseViewer;
	private final PrefilledEntriesViewer prefillViewer;
	private final ViewerUtils utils;

	private NativeLabel entity;
	private NativeLabel formId;

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
		setMargin(false);
		setPadding(false);
		setSpacing(false);	
		FormLayout top = new FormLayout();
		top.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		top.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		formId = new NativeLabel();
		entity = new NativeLabel();
		top.addFormItem(formId,msg.getMessage("EnquiryInvitationViewer.formId"));
		top.addFormItem(entity,msg.getMessage("EnquiryInvitationViewer.entity"));
		add(top);
		add(baseViewer);
		FormLayout prefill = new FormLayout();
		prefill.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		prefill.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		FormItem prefillElement2 = prefill.addFormItem(prefillViewer, msg.getMessage("EnquiryInvitationViewer.enquiryPrefillInfo"));
		prefillViewer.addVisibleChangeListener(v -> prefillElement2.setVisible(v));	
		add(prefill);
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
			entity.setText(getEntityLabel(enqParam.getEntity()));
		}
		BaseForm form = utils.getEnquiryForm(enqParam.getFormPrefill().getFormId());
		if (form == null)
		{
			return;
		}
		formId.setText(form.getName());
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

	@Override
	public Component getComponent()
	{
		return this;
	}
}
