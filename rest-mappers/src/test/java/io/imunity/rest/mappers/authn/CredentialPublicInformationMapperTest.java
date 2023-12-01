/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.authn;

import java.util.function.Function;

import io.imunity.rest.api.types.authn.RestCredentialPublicInformation;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;

public class CredentialPublicInformationMapperTest
		extends MapperTestBase<CredentialPublicInformation, RestCredentialPublicInformation>
{

	@Override
	protected CredentialPublicInformation getFullAPIObject()
	{
		return new CredentialPublicInformation(LocalCredentialState.correct, "det", "ext");
	}

	@Override
	protected RestCredentialPublicInformation getFullRestObject()
	{
		return RestCredentialPublicInformation.builder()
				.withStateDetail("det")
				.withState("correct")
				.withExtraInformation("ext")
				.build();
	}

	@Override
	protected Pair<Function<CredentialPublicInformation, RestCredentialPublicInformation>, Function<RestCredentialPublicInformation, CredentialPublicInformation>> getMapper()
	{
		return Pair.of(CredentialPublicInformationMapper::map, CredentialPublicInformationMapper::map);
	}

}
