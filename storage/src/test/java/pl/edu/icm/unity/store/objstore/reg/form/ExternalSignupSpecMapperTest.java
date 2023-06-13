/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.registration.ExternalSignupSpec;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class ExternalSignupSpecMapperTest extends MapperTestBase<ExternalSignupSpec, DBExternalSignupSpec>
{

	@Override
	protected ExternalSignupSpec getFullAPIObject()
	{
		return new ExternalSignupSpec(List.of(new AuthenticationOptionsSelector("key", "option")));
	}

	@Override
	protected DBExternalSignupSpec getFullDBObject()
	{
		return DBExternalSignupSpec.builder()
				.withSpecs(List.of(DBAuthenticationOptionsSelector.builder()
						.withAuthenticatorKey("key")
						.withOptionKey("option")
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<ExternalSignupSpec, DBExternalSignupSpec>, Function<DBExternalSignupSpec, ExternalSignupSpec>> getMapper()
	{
		return Pair.of(ExternalSignupSpecMapper::map, ExternalSignupSpecMapper::map);
	}

}
