/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.authn;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.authn.RestCredentialPublicInformation;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

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
