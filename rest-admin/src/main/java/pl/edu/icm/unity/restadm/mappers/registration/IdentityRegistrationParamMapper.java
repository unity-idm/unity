/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.Optional;

import io.imunity.rest.api.types.registration.RestIdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.ConfirmationMode;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;

public class IdentityRegistrationParamMapper
{
	public static RestIdentityRegistrationParam map(IdentityRegistrationParam identityRegistrationParam)
	{
		return RestIdentityRegistrationParam.builder()
				.withConfirmationMode(identityRegistrationParam.getConfirmationMode()
						.name())
				.withDescription(identityRegistrationParam.getDescription())
				.withIdentityType(identityRegistrationParam.getIdentityType())
				.withLabel(identityRegistrationParam.getLabel())
				.withOptional(identityRegistrationParam.isOptional())
				.withRetrievalSettings(identityRegistrationParam.getRetrievalSettings()
						.name())
				.withUrlQueryPrefill(Optional.ofNullable(identityRegistrationParam.getUrlQueryPrefill())
						.map(URLQueryPrefillConfigMapper::map)
						.orElse(null))
				.build();
	}

	public static IdentityRegistrationParam map(RestIdentityRegistrationParam registrationParam)
	{
		IdentityRegistrationParam identityRegistrationParam = new IdentityRegistrationParam();
		identityRegistrationParam.setConfirmationMode(ConfirmationMode.valueOf(registrationParam.confirmationMode));
		identityRegistrationParam.setDescription(registrationParam.description);
		identityRegistrationParam.setIdentityType(registrationParam.identityType);
		identityRegistrationParam.setLabel(registrationParam.label);
		identityRegistrationParam.setOptional(registrationParam.optional);
		identityRegistrationParam
				.setRetrievalSettings(ParameterRetrievalSettings.valueOf(registrationParam.retrievalSettings));
		identityRegistrationParam.setUrlQueryPrefill(Optional.ofNullable(registrationParam.urlQueryPrefill)
				.map(URLQueryPrefillConfigMapper::map)
				.orElse(null));
		return identityRegistrationParam;
	}
}
