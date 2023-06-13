/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.time.Instant;
import java.util.Optional;

import pl.edu.icm.unity.base.registration.invite.ComboInvitationParam;

public class ComboInvitationParamMapper
{
	public static DBComboInvitationParam map(ComboInvitationParam enquiryInvitationParam)
	{
		return DBComboInvitationParam.builder()

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

	public static ComboInvitationParam map(DBComboInvitationParam restEnquiryInvitationParam)
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
