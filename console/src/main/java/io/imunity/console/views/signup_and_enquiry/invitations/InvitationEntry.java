/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations;

import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.imunity.vaadin.elements.grid.FilterableEntry;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;

/**
 * Represent grid invitation entry.
 * 
 * @author P.Piernik
 *
 */
class InvitationEntry implements FilterableEntry
{
	private final MessageSource msg;
	final InvitationWithCode invitationWithCode;
	final InvitationParam invitation;

	InvitationEntry(MessageSource msg, InvitationWithCode invitationWithCode)
	{
		this.invitationWithCode = invitationWithCode;
		this.invitation = invitationWithCode.getInvitation();
		this.msg = msg;
	}

	String getType()
	{
		return msg.getMessage("InvitationType." + invitation.getType()
				.toString()
				.toLowerCase());
	}

	String getForm()
	{
		return invitation.getFormsPrefillData()
				.stream()
				.map(i -> i.getFormId())
				.collect(Collectors.joining(", "));

	}

	String getCode()
	{
		return invitationWithCode.getRegistrationCode();
	}

	String getExpiration()
	{
		return TimeUtil.formatStandardInstant(invitation.getExpiration());
	}

	boolean isExpired()
	{
		return Instant.now()
				.isAfter(invitation.getExpiration());
	}

	String getAddress()
	{
		return invitation.getContactAddress() == null ? "-" : invitation.getContactAddress();
	}

	@Override
	public boolean anyFieldContains(String searched, Function<String, String> msg)
	{
		String textLower = searched.toLowerCase();

		if (getAddress() != null && getAddress().toLowerCase()
				.contains(textLower))
			return true;

		if (getExpiration() != null && getExpiration().toLowerCase()
				.contains(textLower))
			return true;

		if (getType() != null && getType().toLowerCase()
				.contains(textLower))
			return true;

		if (getForm() != null && getForm().toLowerCase()
				.contains(textLower))
			return true;

		return false;
	}
}