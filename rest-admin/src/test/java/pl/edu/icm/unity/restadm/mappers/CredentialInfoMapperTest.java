/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.authn.RestCredentialInfo;
import io.imunity.rest.api.types.authn.RestCredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

public class CredentialInfoMapperTest extends MapperTestBase<CredentialInfo, RestCredentialInfo>
{

	@Override
	protected CredentialInfo getAPIObject()
	{
		return new CredentialInfo("cred",
				Map.of("cpi1", new CredentialPublicInformation(LocalCredentialState.correct, "detail", "extraInfo")));
	}

	@Override
	protected RestCredentialInfo getRestObject()
	{
		return RestCredentialInfo.builder()
				.withCredentialRequirementId("cred")
				.withCredentialsState(Map.of("cpi1", RestCredentialPublicInformation.builder()
						.withExtraInformation("extraInfo")
						.withState("correct")
						.withStateDetail("detail")
						.build()))

				.build();
	}

	@Override
	protected Pair<Function<CredentialInfo, RestCredentialInfo>, Function<RestCredentialInfo, CredentialInfo>> getMapper()
	{
		return Pair.of(CredentialInfoMapper::map, CredentialInfoMapper::map);
	}

}
