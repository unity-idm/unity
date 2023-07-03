/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;


import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;

public class InvitationParamMapper
{
	public static InvitationParam map(DBInvitationParam invitation)
	{
		if (invitation instanceof DBRegistrationInvitationParam)
			return RegistrationInvitationParamMapper.map((DBRegistrationInvitationParam) invitation);
		else if (invitation instanceof DBEnquiryInvitationParam)
			return EnquiryInvitationParamMapper.map((DBEnquiryInvitationParam) invitation);
		else if (invitation instanceof DBComboInvitationParam)
			return ComboInvitationParamMapper.map((DBComboInvitationParam) invitation);
		else
			throw new IllegalArgumentException("Illegal invitation type");
	}

	public static DBInvitationParam map(InvitationParam invitation)
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
