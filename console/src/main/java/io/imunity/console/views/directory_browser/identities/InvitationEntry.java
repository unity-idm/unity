/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.identities;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;

import java.util.function.Function;
import java.util.stream.Collectors;

import io.imunity.vaadin.elements.grid.FilterableEntry;


public class InvitationEntry implements FilterableEntry
{
	private final MessageSource msg;
	public final InvitationWithCode invitationWithCode;
	public final InvitationParam invitation;

	InvitationEntry(MessageSource msg, InvitationWithCode invitationWithCode)
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
		return invitation.getFormsPrefillData().stream().map(FormPrefill::getFormId).collect(Collectors.joining(", "));
		
	}

	public String getCode()
	{
		return invitationWithCode.getRegistrationCode();
	}

	public String getExpiration()
	{
		return TimeUtil.formatStandardInstant(invitation.getExpiration());
	}


	public String getAddress()
	{
		return invitation.getContactAddress() == null ? "-" : invitation.getContactAddress();
	}

	@Override
	public boolean anyFieldContains(String searched, Function<String, String>  msg)
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