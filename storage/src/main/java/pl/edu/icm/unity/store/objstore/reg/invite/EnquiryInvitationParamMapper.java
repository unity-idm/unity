/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.time.Instant;
import java.util.Optional;

import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;

public class EnquiryInvitationParamMapper
{
	public static DBEnquiryInvitationParam map(EnquiryInvitationParam enquiryInvitationParam)
	{
		return DBEnquiryInvitationParam.builder()
				.withEntity(enquiryInvitationParam.getEntity())
				.withContactAddress(enquiryInvitationParam.getContactAddress())
				.withInviter(enquiryInvitationParam.getInviterEntity()
						.orElse(null))
				.withType(enquiryInvitationParam.getType()
						.name())
				.withExpiration(enquiryInvitationParam.getExpiration()
						.toEpochMilli())
				.withFormPrefill(Optional.ofNullable(enquiryInvitationParam.getFormPrefill())
						.map(FormPrefillMapper::map)
						.orElse(null))
				.build();
	}

	public static EnquiryInvitationParam map(DBEnquiryInvitationParam restEnquiryInvitationParam)
	{

		return EnquiryInvitationParam.builder()
				.withEntity(restEnquiryInvitationParam.entity)
				.withInviter(restEnquiryInvitationParam.inviter)
				.withContactAddress(restEnquiryInvitationParam.contactAddress)
				.withExpiration(Instant.ofEpochMilli(restEnquiryInvitationParam.expiration))
				.withForm(Optional.ofNullable(restEnquiryInvitationParam.formPrefill)
						.map(FormPrefillMapper::map)
						.orElse(null))
				.build();

	}

}
