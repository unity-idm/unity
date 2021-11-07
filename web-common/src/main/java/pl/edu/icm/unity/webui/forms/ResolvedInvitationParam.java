/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

public class ResolvedInvitationParam
{
	public final String code;
	public final List<Entity> entities;
	public final String contactAddress;
	private final InvitationParam invitationParam;

	ResolvedInvitationParam(List<Entity> entities, String code, InvitationParam invitationParam)
	{
		this.entities =  Collections.unmodifiableList(entities);
		this.invitationParam = invitationParam;
		this.contactAddress = invitationParam.getContactAddress();
		this.code = code;
	}

	public RegistrationInvitationParam getAsRegistration()
	{
		if (invitationParam.getType().equals(InvitationType.REGISTRATION))
			return (RegistrationInvitationParam) invitationParam;

		if (invitationParam.getType().equals(InvitationType.COMBO))
		{
			return ((ComboInvitationParam) invitationParam).getAsRegistration();
		}

		throw new UnsupportedOperationException("Enquiry invitation only");
	}

	public EnquiryInvitationParam getAsEnquiryInvitationParam(Long entity)
	{
		if (invitationParam.getType().equals(InvitationType.ENQUIRY))
			return (EnquiryInvitationParam) invitationParam;

		if (invitationParam.getType().equals(InvitationType.COMBO))
		{
			return ((ComboInvitationParam) invitationParam).getAsEnquiry(entity);
		}

		throw new UnsupportedOperationException("Registration invitation only");
	}

	public boolean canBeProcessedAsEnquiryWithResolvedUser()
	{
		return invitationParam.getType().equals(InvitationType.COMBO) && !entities.isEmpty();
	}
	
	public InvitationType getType()
	{
		return invitationParam.getType();
	}
}
