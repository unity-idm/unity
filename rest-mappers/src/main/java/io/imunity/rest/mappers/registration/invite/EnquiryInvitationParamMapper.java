/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration.invite;

import java.time.Instant;
import java.util.Optional;

import io.imunity.rest.api.types.registration.invite.RestEnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;

public class EnquiryInvitationParamMapper
{
	public static RestEnquiryInvitationParam map(EnquiryInvitationParam enquiryInvitationParam)
	{
		return RestEnquiryInvitationParam.builder()
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

	public static EnquiryInvitationParam map(RestEnquiryInvitationParam restEnquiryInvitationParam)
	{

		return EnquiryInvitationParam.builder()
				.withEntity(restEnquiryInvitationParam.entity)
				.withInviter(restEnquiryInvitationParam.inviter)
				.withContactAddress(restEnquiryInvitationParam.contactAddress)
				.withExpiration(Instant.ofEpochMilli(restEnquiryInvitationParam.expiration))
				.withForm(Optional.ofNullable(restEnquiryInvitationParam.formPrefill)
						.map(FormPrefillMapper::map)
						.map(fp ->
						{
							fp.setFormType(FormType.ENQUIRY);
							return fp;
						})
						.orElse(null))
				.build();

	}

}
