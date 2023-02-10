/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration.invite;

import io.imunity.rest.api.types.registration.invite.RestInvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

public class InvitationWithCodeMapper
{
	public static RestInvitationWithCode map(InvitationWithCode invitationWithCode)
	{
		return RestInvitationWithCode.builder()
				.withCreationTime(invitationWithCode.getCreationTime())
				.withLastSentTime(invitationWithCode.getLastSentTime())
				.withNumberOfSends(invitationWithCode.getNumberOfSends())
				.withRegistrationCode(invitationWithCode.getRegistrationCode())
				.withInvitation(InvitationParamMapper.map(invitationWithCode.getInvitation()))
				.build();
	}

	public static InvitationWithCode map(RestInvitationWithCode restInvitationWithCode)
	{

		InvitationWithCode invitationWithCode = new InvitationWithCode(
				InvitationParamMapper.map(restInvitationWithCode.invitation), restInvitationWithCode.registrationCode,
				restInvitationWithCode.lastSentTime, restInvitationWithCode.numberOfSends);
		invitationWithCode.setCreationTime(restInvitationWithCode.creationTime);
		return invitationWithCode;

	}

}
