/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.viewer;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
class ComboInvitationViewer extends VerticalLayout implements InvitationViewer
{
	private final MessageSource msg;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final CommonInvitationFieldViewer baseViewer;
	private final PrefilledEntriesViewer regPrefillViewer;
	private final PrefilledEntriesViewer enqPrefillViewer;
	private final ViewerUtils utils;

	private NativeLabel regFormId;
	private NativeLabel enqFormId;

	ComboInvitationViewer(PublicRegistrationURLSupport publicRegistrationURLSupport, MessageSource msg,
			CommonInvitationFieldViewer baseViewer, PrefilledEntriesViewer regPrefillViewer,
			PrefilledEntriesViewer enqPrefillViewer, ViewerUtils utils)
	{
		this.msg = msg;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
		this.utils = utils;
		this.baseViewer = baseViewer;
		this.regPrefillViewer = regPrefillViewer;
		this.enqPrefillViewer = enqPrefillViewer;
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
		regFormId = new NativeLabel();
		enqFormId = new NativeLabel();
		top.addFormItem(regFormId,msg.getMessage("RegistrationInvitationViewer.formId"));
		top.addFormItem(enqFormId, msg.getMessage("EnquiryInvitationViewer.formId"));
		add(top);
		add(baseViewer);
		FormLayout prefill = new FormLayout();
		prefill.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		prefill.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		FormItem prefillElement1 = prefill.addFormItem(regPrefillViewer, msg.getMessage("RegistrationInvitationViewer.registrationPrefillInfo"));
		regPrefillViewer.addVisibleChangeListener(v -> prefillElement1.setVisible(v));
		FormItem prefillElement2 = prefill.addFormItem(enqPrefillViewer, msg.getMessage("EnquiryInvitationViewer.enquiryPrefillInfo"));
		enqPrefillViewer.addVisibleChangeListener(v -> prefillElement2.setVisible(v));	
		add(prefill);
	
	}

	public void setInput(InvitationWithCode invitationWithCode) throws IllegalFormTypeException
	{
		if (invitationWithCode == null)
		{
			return;
		}
		ComboInvitationParam comboParam = (ComboInvitationParam) invitationWithCode.getInvitation();
		BaseForm regForm = utils.getRegistrationForm(comboParam.getRegistrationFormPrefill().getFormId());
		if (regForm == null)
		{
			return;
		}
		BaseForm enqForm = utils.getEnquiryForm(comboParam.getEnquiryFormPrefill().getFormId());
		if (enqForm == null)
		{
			return;
		}
		regFormId.setText(regForm.getName());
		regPrefillViewer.setInput(regForm, comboParam.getRegistrationFormPrefill());

		enqFormId.setText(enqForm.getName());
		enqPrefillViewer.setInput(enqForm, comboParam.getEnquiryFormPrefill());

		baseViewer.setInput(invitationWithCode, utils.getChannel(regForm),
				publicRegistrationURLSupport.getPublicFormLink(regForm.getName(), FormType.REGISTRATION,
						invitationWithCode.getRegistrationCode()),
				Stream.concat(comboParam.getRegistrationFormPrefill().getMessageParams().entrySet().stream(),
						comboParam.getEnquiryFormPrefill().getMessageParams().entrySet().stream()).distinct()
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

	}
	
	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public InvitationType getSupportedType()
	{
		return InvitationType.COMBO;
	}
}
