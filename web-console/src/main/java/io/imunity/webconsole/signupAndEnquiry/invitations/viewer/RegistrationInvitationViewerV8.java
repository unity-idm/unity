/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

@PrototypeComponent
class RegistrationInvitationViewerV8 extends CustomComponent implements InvitationViewer
{
	private final MessageSource msg;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final CommonInvitationFieldViewerV8 baseViewer;
	private final PrefilledEntriesViewerV8 prefillViewer;
	private final ViewerUtilsV8 utils;

	private Label expectedIdentity;
	private Label formId;

	RegistrationInvitationViewerV8(PublicRegistrationURLSupport publicRegistrationURLSupport, MessageSource msg,
			CommonInvitationFieldViewerV8 baseViewer, PrefilledEntriesViewerV8 prefillViewer, ViewerUtilsV8 utils)
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
		VerticalLayout main = new VerticalLayout();
		main.setMargin(new MarginInfo(false, true));
		setCompositionRoot(main);
		FormLayoutWithFixedCaptionWidth top = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		top.setMargin(false);

		formId = new Label();
		formId.setCaption(msg.getMessage("RegistrationInvitationViewer.formId"));

		expectedIdentity = new Label();
		expectedIdentity.setWidth(100, Unit.PERCENTAGE);
		expectedIdentity.setCaption(msg.getMessage("RegistrationInvitationViewer.expectedIdentity"));

		top.addComponents(formId, expectedIdentity);
		main.addComponent(top);
		main.addComponent(baseViewer);
		FormLayoutWithFixedCaptionWidth prefill = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		prefill.setMargin(false);
		prefillViewer.setCaption(msg.getMessage("RegistrationInvitationViewer.registrationPrefillInfo"));
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

		RegistrationInvitationParam regParam = (RegistrationInvitationParam) invitationWithCode.getInvitation();
		expectedIdentity.setVisible(regParam.getExpectedIdentity() != null);
		if (regParam.getExpectedIdentity() != null)
		{
			expectedIdentity.setValue(regParam.getExpectedIdentity().toString());
		}

		BaseForm form = utils.getRegistrationForm(regParam.getFormPrefill().getFormId());
		if (form == null)
		{
			return;
		}
		formId.setValue(form.getName());
		prefillViewer.setInput(form, regParam.getFormPrefill());
		baseViewer
				.setInput(invitationWithCode, utils.getChannel(form),
						publicRegistrationURLSupport.getPublicFormLink(form.getName(), FormType.REGISTRATION,
								invitationWithCode.getRegistrationCode()),
						regParam.getFormPrefill().getMessageParams());

	}

	@Override
	public InvitationType getSupportedType()
	{
		return InvitationType.REGISTRATION;
	}
}
