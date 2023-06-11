/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class CredentialRegistrationParamTest
		extends MapperTestBase<CredentialRegistrationParam, DBCredentialRegistrationParam>
{

	@Override
	protected CredentialRegistrationParam getFullAPIObject()
	{

		return new CredentialRegistrationParam("name", "label", "desc");
	}

	@Override
	protected DBCredentialRegistrationParam getFullDBObject()
	{

		return DBCredentialRegistrationParam.builder()
				.withCredentialName("name")
				.withDescription("desc")
				.withLabel("label")
				.build();

	}

	@Override
	protected Pair<Function<CredentialRegistrationParam, DBCredentialRegistrationParam>, Function<DBCredentialRegistrationParam, CredentialRegistrationParam>> getMapper()
	{
		return Pair.of(CredentialRegistrationParamMapper::map, CredentialRegistrationParamMapper::map);
	}

}
