/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration.invite;

import io.imunity.rest.api.types.registration.invite.RestComboInvitationParam;
import io.imunity.rest.api.types.registration.invite.RestEnquiryInvitationParam;
import io.imunity.rest.api.types.registration.invite.RestInvitationParam;
import io.imunity.rest.api.types.registration.invite.RestRegistrationInvitationParam;
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

public class InvitationParamMapper
{
	public static InvitationParam map(RestInvitationParam invitation)
	{
		if (invitation instanceof RestRegistrationInvitationParam)
			return RegistrationInvitationParamMapper.map((RestRegistrationInvitationParam) invitation);
		else if (invitation instanceof RestEnquiryInvitationParam)
			return EnquiryInvitationParamMapper.map((RestEnquiryInvitationParam) invitation);
		else if (invitation instanceof RestComboInvitationParam)
			return ComboInvitationParamMapper.map((RestComboInvitationParam) invitation);
		else
			throw new IllegalArgumentException("Illegal invitation type");
	}

	public static RestInvitationParam map(InvitationParam invitation)
	{
		if (invitation instanceof RegistrationInvitationParam)
			return RegistrationInvitationParamMapper.map((RegistrationInvitationParam) invitation);
		else if (invitation instanceof EnquiryInvitationParam)
			return EnquiryInvitationParamMapper.map((EnquiryInvitationParam) invitation);
		else if (invitation instanceof ComboInvitationParam)
			return ComboInvitationParamMapper.map((ComboInvitationParam) invitation);
		else
			throw new IllegalArgumentException("Illegal invitation type");
	}
}
