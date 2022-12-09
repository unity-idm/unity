/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.reg;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.FreemarkerUtils;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class SwitchToEnquiryComponentProvider
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

		Map<String, Object> paramsWithSwitch = new HashMap<>();
		paramsWithSwitch.putAll(params);
		paramsWithSwitch.put(SWITCH_TO_ENQUIRY_START_VAR, SwitchToEnquiryComponentProvider.SWITCH_START);
		paramsWithSwitch.put(SWITCH_TO_ENQUIRY_END_VAR, SwitchToEnquiryComponentProvider.SWITCH_END);
		String switchInfo = switchText == null ? null
				: FreemarkerUtils.processStringTemplate(paramsWithSwitch, switchText.getValue(msg));
		if (switchInfo == null || switchInfo.isEmpty())
			return Optional.empty();

		String linkDisp = switchInfo.substring(switchInfo.indexOf(SWITCH_START),
				switchInfo.indexOf(SWITCH_END) == -1 ? switchInfo.length() : switchInfo.indexOf(SWITCH_END));
		switchInfo = switchInfo.replace(linkDisp, getLink(linkDisp, invitation));
		switchInfo = switchInfo.replace(SWITCH_START, "");
		switchInfo = switchInfo.replace(SWITCH_END, "");
		Label label = new Label();
		label.setContentMode(ContentMode.HTML);
		label.addStyleName("wrap-line");
		label.setValue(switchInfo);
		label.addStyleName("u-reg-info");

		return Optional.of(label);
	}

	private String getLink(String disp, ResolvedInvitationParam invitation)
	{
		String link = publicRegistrationURLSupport.getWellknownEnquiryLink(
				invitation.getAsEnquiryInvitationParamWithAnonymousEntity().getFormPrefill().getFormId(), invitation.code);
		return "<a href=\"" + link + "\">" + disp + "</a>";
	}
}
