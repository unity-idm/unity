/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

public class IdentityRegistrationParamMapperTest
		extends MapperTestBase<IdentityRegistrationParam, DBIdentityRegistrationParam>
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
	protected DBIdentityRegistrationParam getFullDBObject()
	{
		return DBIdentityRegistrationParam.builder()
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
				.withIdentityType("type")
				.withLabel("label")
				.withOptional(false)
				.withRetrievalSettings("automatic")
				.withUrlQueryPrefill(DBurlQueryPrefillConfig.builder()
						.withMode("DEFAULT")
						.withParamName("param")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<IdentityRegistrationParam, DBIdentityRegistrationParam>, Function<DBIdentityRegistrationParam, IdentityRegistrationParam>> getMapper()
	{
		return Pair.of(IdentityRegistrationParamMapper::map, IdentityRegistrationParamMapper::map);
	}

}
