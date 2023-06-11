/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration.invite;

import java.time.Instant;
import java.util.Optional;

import io.imunity.rest.api.types.registration.invite.RestRegistrationInvitationParam;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.invite.RegistrationInvitationParam;

public class RegistrationInvitationParamMapper
{
	public static RestRegistrationInvitationParam map(RegistrationInvitationParam registrationInvitationParam)
	{
		return RestRegistrationInvitationParam.builder()
				.withContactAddress(registrationInvitationParam.getContactAddress())
				.withExpectedIdentity(Optional.ofNullable(registrationInvitationParam.getExpectedIdentity())
						.map(ExpectedIdentityMapper::map)
						.orElse(null))
				.withInviter(registrationInvitationParam.getInviterEntity()
						.orElse(null))
				.withType(registrationInvitationParam.getType()
						.name())
				.withExpiration(registrationInvitationParam.getExpiration()
						.toEpochMilli())
				.withFormPrefill(Optional.ofNullable(registrationInvitationParam.getFormPrefill())
						.map(FormPrefillMapper::map)
						.orElse(null))
				.build();
	}

	public static RegistrationInvitationParam map(RestRegistrationInvitationParam restRegistrationInvitationParam)
	{

		return RegistrationInvitationParam.builder()
				.withInviter(restRegistrationInvitationParam.inviter)
				.withExpectedIdentity(Optional.ofNullable(restRegistrationInvitationParam.expectedIdentity)
						.map(ExpectedIdentityMapper::map)
						.orElse(null))
				.withContactAddress(restRegistrationInvitationParam.contactAddress)
				.withExpiration(Instant.ofEpochMilli(restRegistrationInvitationParam.expiration))
				.withForm(Optional.ofNullable(restRegistrationInvitationParam.formPrefill)
						.map(FormPrefillMapper::map)
						.map(fp ->
						{
							fp.setFormType(FormType.REGISTRATION);
							return fp;
						})
						.orElse(null))
				.build();

	}

}
