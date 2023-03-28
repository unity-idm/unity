/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;

public class IdentityRegistrationParamMapper
{
	public static DBIdentityRegistrationParam map(IdentityRegistrationParam identityRegistrationParam)
	{
		return DBIdentityRegistrationParam.builder()
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

	public static IdentityRegistrationParam map(DBIdentityRegistrationParam registrationParam)
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