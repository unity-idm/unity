/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

@PrototypeComponent
class RegistrationInvitationViewer extends CustomComponent implements InvitationViewer
{
	private final MessageSource msg;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final CommonInvitationFieldViewer baseViewer;
	private final PrefilledEntriesViewer prefillViewer;
	private final ViewerUtils utils;

	private Label expectedIdentity;
	private Label formId;

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
