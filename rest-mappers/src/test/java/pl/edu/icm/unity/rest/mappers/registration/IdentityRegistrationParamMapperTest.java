/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestIdentityRegistrationParam;
import io.imunity.rest.api.types.registration.RestURLQueryPrefillConfig;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

public class IdentityRegistrationParamMapperTest
		extends MapperTestBase<IdentityRegistrationParam, RestIdentityRegistrationParam>
{

	@Override
	protected IdentityRegistrationParam getFullAPIObject()
	{
		IdentityRegistrationParam identityRegistrationParam = new IdentityRegistrationParam();
		identityRegistrationParam.setConfirmationMode(ConfirmationMode.CONFIRMED);
		identityRegistrationParam.setDescription("desc");
		identityRegistrationParam.setIdentityType("type");
		identityRegistrationParam.setLabel("label");
		identityRegistrationParam.setOptional(false);
		identityRegistrationParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		identityRegistrationParam.setUrlQueryPrefill(new URLQueryPrefillConfig("param", PrefilledEntryMode.DEFAULT));
		return identityRegistrationParam;
	}

	@Override
	protected RestIdentityRegistrationParam getFullRestObject()
	{
		return RestIdentityRegistrationParam.builder()
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
				.withIdentityType("type")
				.withLabel("label")
				.withOptional(false)
				.withRetrievalSettings("automatic")
				.withUrlQueryPrefill(RestURLQueryPrefillConfig.builder()
						.withMode("DEFAULT")
						.withParamName("param")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<IdentityRegistrationParam, RestIdentityRegistrationParam>, Function<RestIdentityRegistrationParam, IdentityRegistrationParam>> getMapper()
	{
		return Pair.of(IdentityRegistrationParamMapper::map, IdentityRegistrationParamMapper::map);
	}

}
