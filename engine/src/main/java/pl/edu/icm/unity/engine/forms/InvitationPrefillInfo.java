/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.util.Optional;

import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

public class InvitationPrefillInfo
{
	private Optional<InvitationWithCode> invitation;
	
	public InvitationPrefillInfo()
	{
		this(null);
	}

	public InvitationPrefillInfo(InvitationWithCode invitation)
	{
		this.invitation = Optional.ofNullable(invitation);
	}

	public boolean isByInvitation()
	{
		return !invitation.isEmpty();
	}
	
	public Optional<InvitationWithCode> getInvitation()
	{
		return invitation;
	}
}
