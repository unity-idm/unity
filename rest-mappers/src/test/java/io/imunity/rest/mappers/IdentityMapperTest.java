/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers;

import java.util.Date;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.basic.RestIdentity;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.Identity;

public class IdentityMapperTest extends MapperWithMinimalTestBase<Identity, RestIdentity>
{

	@Override
	protected Identity getFullAPIObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		Identity ret = new Identity("username", "name", 1, "name");
		ret.setMetadata(meta);
		ret.setRealm("realm");
		ret.setRemoteIdp("remoteIdp");
		ret.setTarget("target");
		ret.setTranslationProfile("translationProfile");
		ret.setUpdateTs(new Date(2));
		ret.setCreationTs(new Date(1));

		return ret;
	}

	@Override
	protected RestIdentity getFullRestObject()
	{
		return RestIdentity.builder()
				.withCreationTs(new Date(1))
				.withUpdateTs(new Date(2))
				.withComparableValue("name")
				.withEntityId(1)
				.withTypeId("username")
				.withValue("name")
				.withConfirmationInfo(RestConfirmationInfo.builder()
						.withConfirmed(false)
						.build())
				.withRealm("realm")
				.withTranslationProfile("translationProfile")
				.withTarget("target")
				.withMetadata(Constants.MAPPER.createObjectNode())
				.withRemoteIdp("remoteIdp")
				.build();
	}

	@Override
	protected Identity getMinAPIObject()
	{
		Identity ret = new Identity("username", "name", 1, "name");
		ret.setUpdateTs(new Date(2));
		ret.setCreationTs(new Date(1));
		return ret;
	}

	@Override
	protected RestIdentity getMinRestObject()
	{
		return RestIdentity.builder()
				.withCreationTs(new Date(1))
				.withUpdateTs(new Date(2))
				.withComparableValue("name")
				.withEntityId(1)
				.withTypeId("username")
				.withValue("name")
				.withConfirmationInfo(RestConfirmationInfo.builder()
						.withConfirmed(false)
						.build())
				.build();
	}

	@Override
	protected Pair<Function<Identity, RestIdentity>, Function<RestIdentity, Identity>> getMapper()
	{
		return Pair.of(IdentityMapper::map, IdentityMapper::map);
	}
}
