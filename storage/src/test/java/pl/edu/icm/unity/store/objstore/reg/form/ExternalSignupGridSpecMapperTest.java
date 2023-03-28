/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec.AuthnGridSettings;

public class ExternalSignupGridSpecMapperTest extends MapperTestBase<ExternalSignupGridSpec, DBExternalSignupGridSpec>
{

	@Override
	protected ExternalSignupGridSpec getFullAPIObject()
	{
		return new ExternalSignupGridSpec(List.of(new AuthenticationOptionsSelector("key", "option")),
				new AuthnGridSettings(true, 1));
	}

	@Override
	protected DBExternalSignupGridSpec getFullDBObject()
	{
		return DBExternalSignupGridSpec.builder()
				.withSpecs(List.of(DBAuthenticationOptionsSelector.builder()
						.withAuthenticatorKey("key")
						.withOptionKey("option")
						.build()))
				.withGridSettings(DBAuthnGridSettings.builder()
						.withHeight(1)
						.withSearchable(true)
						.build())
				.build();
	}

	@Override
	protected Pair<Function<ExternalSignupGridSpec, DBExternalSignupGridSpec>, Function<DBExternalSignupGridSpec, ExternalSignupGridSpec>> getMapper()
	{
		return Pair.of(ExternalSignupGridSpecMapper::map, ExternalSignupGridSpecMapper::map);
	}

}
