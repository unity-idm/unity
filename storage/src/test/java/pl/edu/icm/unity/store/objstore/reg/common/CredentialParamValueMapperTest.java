/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.CredentialParamValue;

public class CredentialParamValueMapperTest extends MapperTestBase<CredentialParamValue, DBCredentialParamValue>
{

	@Override
	protected CredentialParamValue getFullAPIObject()
	{
		return new CredentialParamValue("credential", "secret");
	}

	@Override
	protected DBCredentialParamValue getFullDBObject()
	{
		return DBCredentialParamValue.builder()
				.withCredentialId("credential")
				.withSecrets("secret")
				.build();
	}

	@Override
	protected Pair<Function<CredentialParamValue, DBCredentialParamValue>, Function<DBCredentialParamValue, CredentialParamValue>> getMapper()
	{
		return Pair.of(CredentialParamValueMapper::map, CredentialParamValueMapper::map);
	}

}
