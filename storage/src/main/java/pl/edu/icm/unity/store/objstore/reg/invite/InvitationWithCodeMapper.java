/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;

public class InvitationWithCodeMapper
{
	public static DBInvitationWithCode map(InvitationWithCode invitationWithCode)
	{
		return DBInvitationWithCode.builder()
				.withCreationTime(invitationWithCode.getCreationTime())
				.withLastSentTime(invitationWithCode.getLastSentTime())
				.withNumberOfSends(invitationWithCode.getNumberOfSends())
				.withRegistrationCode(invitationWithCode.getRegistrationCode())
				.withInvitation(InvitationParamMapper.map(invitationWithCode.getInvitation()))
				.build();
	}

	public static InvitationWithCode map(DBInvitationWithCode restInvitationWithCode)
	{

		InvitationWithCode invitationWithCode = new InvitationWithCode(
				InvitationParamMapper.map(restInvitationWithCode.invitation), restInvitationWithCode.registrationCode,
				restInvitationWithCode.lastSentTime, restInvitationWithCode.numberOfSends);
		invitationWithCode.setCreationTime(restInvitationWithCode.creationTime);
		return invitationWithCode;

	}

}
