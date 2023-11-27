/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration.invite;

import java.time.Instant;
import java.util.Optional;

import io.imunity.rest.api.types.registration.invite.RestComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;

public class ComboInvitationParamMapper
{
	public static RestComboInvitationParam map(ComboInvitationParam enquiryInvitationParam)
	{
		return RestComboInvitationParam.builder()

				.withContactAddress(enquiryInvitationParam.getContactAddress())
				.withInviter(enquiryInvitationParam.getInviterEntity()
						.orElse(null))
				.withType(enquiryInvitationParam.getType()
						.name())
				.withExpiration(enquiryInvitationParam.getExpiration()
						.toEpochMilli())
				.withEnquiryFormPrefill(Optional.ofNullable(enquiryInvitationParam.getEnquiryFormPrefill())
						.map(FormPrefillMapper::map)
						.orElse(null))
				.withRegistrationFormPrefill(Optional.ofNullable(enquiryInvitationParam.getRegistrationFormPrefill())
						.map(FormPrefillMapper::map)
						.orElse(null))
				.build();
	}

	public static ComboInvitationParam map(RestComboInvitationParam restEnquiryInvitationParam)
	{
		return ComboInvitationParam.builder()
				.withInviter(restEnquiryInvitationParam.inviter)
				.withContactAddress(restEnquiryInvitationParam.contactAddress)
				.withExpiration(Instant.ofEpochMilli(restEnquiryInvitationParam.expiration))
				.withEnquiryForm(Optional.ofNullable(restEnquiryInvitationParam.enquiryFormPrefill)
						.map(FormPrefillMapper::map)
						.orElse(null))
				.withRegistrationForm(Optional.ofNullable(restEnquiryInvitationParam.registrationFormPrefill)
						.map(FormPrefillMapper::map)
						.orElse(null))
				.build();
	}

}
