/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webadmin.reg.invitations;

import java.time.Instant;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

/**
 * Represent grid invitation entry.
 * 
 * @author P.Piernik
 *
 */
public class InvitationEntry implements FilterableEntry
{
	private UnityMessageSource msg;
	public final InvitationWithCode invitationWithCode;
	public final InvitationParam invitation;

	public InvitationEntry(UnityMessageSource msg, InvitationWithCode invitationWithCode)
	{
		this.invitationWithCode = invitationWithCode;
		this.invitation = invitationWithCode.getInvitation();
		this.msg = msg;
	}

	public String getType()
	{
		return msg.getMessage("InvitationType." + invitation.getType().toString().toLowerCase());
	}

	public String getForm()
	{
		return invitation.getFormId();
	}

	public String getCode()
	{
		return invitationWithCode.getRegistrationCode();
	}

	public String getExpiration()
	{
		return TimeUtil.formatStandardInstant(invitation.getExpiration());
	}

	public boolean isExpired()
	{
		return Instant.now().isAfter(invitation.getExpiration());
	}

	public String getAddress()
	{
		return invitation.getContactAddress() == null ? "-" : invitation.getContactAddress();
	}

	@Override
	public boolean anyFieldContains(String searched, UnityMessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (getAddress() != null && getAddress().toLowerCase().contains(textLower))
			return true;

		if (getExpiration() != null && getExpiration().toLowerCase().contains(textLower))
			return true;

		if (getType() != null && getType().toLowerCase().contains(textLower))
			return true;

		if (getForm() != null && getForm().toLowerCase().contains(textLower))
			return true;

		return false;
	}
}