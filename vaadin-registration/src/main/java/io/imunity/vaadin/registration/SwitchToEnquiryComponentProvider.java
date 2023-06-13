/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.registration;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.FreemarkerUtils;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
class SwitchToEnquiryComponentProvider
{
	private final MessageSource msg;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;

	private static final String SWITCH_START = "__switch_start";
	private static final String SWITCH_END = "__switch_end";
	private static final String SWITCH_TO_ENQUIRY_START_VAR = "switch_start";
	private static final String SWITCH_TO_ENQUIRY_END_VAR = "switch_end";

	SwitchToEnquiryComponentProvider(MessageSource msg, PublicRegistrationURLSupport publicRegistrationURLSupport)
	{
		this.msg = msg;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
	}

	Optional<Label> getSwitchToEnquiryLabel(I18nString switchText, ResolvedInvitationParam invitation,
	                                        Map<String, Object> params)
	{
		if (invitation == null || !invitation.getType().equals(InvitationType.COMBO) || switchText == null)
		{
			return Optional.empty();
		}

		Map<String, Object> paramsWithSwitch = new HashMap<>(params);
		paramsWithSwitch.put(SWITCH_TO_ENQUIRY_START_VAR, SwitchToEnquiryComponentProvider.SWITCH_START);
		paramsWithSwitch.put(SWITCH_TO_ENQUIRY_END_VAR, SwitchToEnquiryComponentProvider.SWITCH_END);
		String switchInfo = FreemarkerUtils.processStringTemplate(paramsWithSwitch, switchText.getValue(msg));
		if (switchInfo == null || switchInfo.isEmpty())
			return Optional.empty();

		String linkText = switchInfo.substring(
				switchInfo.indexOf(SWITCH_START),
				!switchInfo.contains(SWITCH_END) ? switchInfo.length() : switchInfo.indexOf(SWITCH_END)
		).replace(SWITCH_START, "");
		String startText = switchInfo.substring(0, switchInfo.indexOf(SWITCH_START));
		String endText = switchInfo.substring(
				!switchInfo.contains(SWITCH_END) ? switchInfo.length() : switchInfo.indexOf(SWITCH_END) + SWITCH_END.length()
		);
		Label label = new Label(startText);
		label.add(new Anchor(getLink(invitation), linkText));
		label.add(endText);

		return Optional.of(label);
	}

	private String getLink(ResolvedInvitationParam invitation)
	{
		return publicRegistrationURLSupport.getWellknownEnquiryLink(
				invitation.getAsEnquiryInvitationParamWithAnonymousEntity().getFormPrefill().getFormId(), invitation.code);
	}
}
