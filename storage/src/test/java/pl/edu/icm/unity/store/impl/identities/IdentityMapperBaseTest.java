/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identities;

import java.util.Date;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.DBConfirmationInfo;
import pl.edu.icm.unity.types.basic.Identity;

public class IdentityMapperBaseTest extends MapperTestBase<Identity, DBIdentityBase>
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
	protected DBIdentityBase getFullDBObject()
	{
		return DBIdentityBase.builder()
				.withCreationTs(new Date(1))
				.withUpdateTs(new Date(2))
				.withComparableValue("name")
				.withValue("name")
				.withConfirmationInfo(DBConfirmationInfo.builder()
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
	protected Pair<Function<Identity, DBIdentityBase>, Function<DBIdentityBase, Identity>> getMapper()
	{
		return Pair.of(IdentityBaseMapper::map, i -> IdentityBaseMapper.map(i, "username", 1));
	}
}
