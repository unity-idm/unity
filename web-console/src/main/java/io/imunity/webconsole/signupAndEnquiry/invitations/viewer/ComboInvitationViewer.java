/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

@PrototypeComponent
class ComboInvitationViewer extends CustomComponent implements InvitationViewer
{
	private final MessageSource msg;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final CommonInvitationFieldViewer baseViewer;
	private final PrefilledEntriesViewer regPrefillViewer;
	private final PrefilledEntriesViewer enqPrefillViewer;
	private final ViewerUtils utils;

	private Label regFormId;
	private Label enqFormId;

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
		VerticalLayout main = new VerticalLayout();
		main.setMargin(new MarginInfo(false, true));
		setCompositionRoot(main);
		FormLayoutWithFixedCaptionWidth top = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		top.setMargin(false);

		regFormId = new Label();
		regFormId.setCaption(msg.getMessage("RegistrationInvitationViewer.formId"));

		enqFormId = new Label();
		enqFormId.setCaption(msg.getMessage("EnquiryInvitationViewer.formId"));

		top.addComponents(regFormId, enqFormId);
		main.addComponent(top);
		main.addComponent(baseViewer);

		FormLayoutWithFixedCaptionWidth prefill = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		prefill.setMargin(false);
		enqPrefillViewer.setCaption(msg.getMessage("InvitationEditor.enquiryPrefillInfo"));
		regPrefillViewer.setCaption(msg.getMessage("EnquiryInvitationViewer.enquiryPrefillInfo"));
		prefill.addComponents(regPrefillViewer, enqPrefillViewer);
		main.addComponent(prefill);

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
		regFormId.setValue(regForm.getName());
		regPrefillViewer.setInput(regForm, comboParam.getRegistrationFormPrefill());

		enqFormId.setValue(enqForm.getName());
		enqPrefillViewer.setInput(enqForm, comboParam.getEnquiryFormPrefill());

		baseViewer.setInput(invitationWithCode, utils.getChannel(regForm),
				publicRegistrationURLSupport.getPublicFormLink(regForm.getName(), FormType.REGISTRATION,
						invitationWithCode.getRegistrationCode()),
				Stream.concat(comboParam.getRegistrationFormPrefill().getMessageParams().entrySet().stream(),
						comboParam.getEnquiryFormPrefill().getMessageParams().entrySet().stream()).distinct()
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

	}

	@Override
	public InvitationType getSupportedType()
	{
		return InvitationType.COMBO;
	}
}
