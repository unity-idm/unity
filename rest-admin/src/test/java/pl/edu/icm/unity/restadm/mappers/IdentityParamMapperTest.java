/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class IdentityParamMapperTest extends MapperWithMinimalTestBase<IdentityParam, RestIdentityParam>
{

	@Override
	protected IdentityParam getFullAPIObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		IdentityParam idParam1 = new IdentityParam("email", "test@wp.pl", "remoteIdp", "Profile");
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(true);
		confirmationInfo.setSentRequestAmount(1);
		confirmationInfo.setConfirmationDate(1L);
		idParam1.setConfirmationInfo(confirmationInfo);
		idParam1.setRealm("realm");
		idParam1.setTarget("target");
		idParam1.setMetadata(meta);
		return idParam1;
	}

	@Override
	protected RestIdentityParam getFullRestObject()
	{
		return RestIdentityParam.builder()
				.withValue("test@wp.pl")
				.withTypeId("email")
				.withRealm("realm")
				.withRemoteIdp("remoteIdp")
				.withTarget("target")
				.withMetadata(Constants.MAPPER.createObjectNode())
				.withTranslationProfile("Profile")
				.withConfirmationInfo(RestConfirmationInfo.builder()
						.withSentRequestAmount(1)
						.withConfirmed(true)
						.withConfirmationDate(1L)
						.build())
				.build();
	}

	@Override
	protected IdentityParam getMinAPIObject()
	{
		return new IdentityParam("email", "test@wp.pl");
	}

	@Override
	protected RestIdentityParam getMinRestObject()
	{
		return RestIdentityParam.builder()
				.withValue("test@wp.pl")
				.withTypeId("email")
				.build();
	}

	@Override
	protected Pair<Function<IdentityParam, RestIdentityParam>, Function<RestIdentityParam, IdentityParam>> getMapper()
	{

		return Pair.of(IdentityParamMapper::map, IdentityParamMapper::map);

	}
}
