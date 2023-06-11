/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.time.Instant;
import java.util.Optional;

import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.invite.RegistrationInvitationParam;

public class RegistrationInvitationParamMapper
{
	public static DBRegistrationInvitationParam map(RegistrationInvitationParam registrationInvitationParam)
	{
		return DBRegistrationInvitationParam.builder()
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

	public static RegistrationInvitationParam map(DBRegistrationInvitationParam restRegistrationInvitationParam)
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
