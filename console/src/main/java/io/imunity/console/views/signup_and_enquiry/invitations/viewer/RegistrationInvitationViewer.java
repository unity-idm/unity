/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.viewer;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
class RegistrationInvitationViewer extends VerticalLayout implements InvitationViewer
{
	private final MessageSource msg;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final CommonInvitationFieldViewer baseViewer;
	private final PrefilledEntriesViewer prefillViewer;
	private final ViewerUtils utils;

	private NativeLabel expectedIdentity;
	private NativeLabel formId;

	RegistrationInvitationViewer(PublicRegistrationURLSupport publicRegistrationURLSupport, MessageSource msg,
			CommonInvitationFieldViewer baseViewer, PrefilledEntriesViewer prefillViewer, ViewerUtils utils)
	{
		this.msg = msg;
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
		expectedIdentity = new NativeLabel();
		top.addFormItem(formId,msg.getMessage("RegistrationInvitationViewer.formId"));
		top.addFormItem(expectedIdentity,msg.getMessage("RegistrationInvitationViewer.expectedIdentity"));		
		add(top);
		add(baseViewer);
		FormLayout prefill = new FormLayout();
		prefill.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		prefill.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		FormItem prefillElement = prefill.addFormItem(prefillViewer, msg.getMessage("RegistrationInvitationViewer.registrationPrefillInfo"));
		prefillViewer.addVisibleChangeListener(v -> prefillElement.setVisible(v));
	
		
		add(prefill);
	}

	public void setInput(InvitationWithCode invitationWithCode) throws IllegalFormTypeException
	{

		if (invitationWithCode == null)
		{
			setVisible(false);
			return;
		}

		RegistrationInvitationParam regParam = (RegistrationInvitationParam) invitationWithCode.getInvitation();
		expectedIdentity.setVisible(regParam.getExpectedIdentity() != null);
		if (regParam.getExpectedIdentity() != null)
		{
			expectedIdentity.setText(regParam.getExpectedIdentity().toString());
		}

		BaseForm form = utils.getRegistrationForm(regParam.getFormPrefill().getFormId());
		if (form == null)
		{
			return;
		}
		formId.setText(form.getName());
		prefillViewer.setInput(form, regParam.getFormPrefill());
		baseViewer
				.setInput(invitationWithCode, utils.getChannel(form),
						publicRegistrationURLSupport.getPublicFormLink(form.getName(), FormType.REGISTRATION,
								invitationWithCode.getRegistrationCode()),
						regParam.getFormPrefill().getMessageParams());

	}
	
	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public InvitationType getSupportedType()
	{
		return InvitationType.REGISTRATION;
	}
}
