/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identities;

import java.util.Date;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;

public class IdentityMapperTest extends MapperWithMinimalTestBase<Identity, DBIdentity>
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
	protected DBIdentity getFullDBObject()
	{
		return DBIdentity.builder()
				.withCreationTs(new Date(1))
				.withUpdateTs(new Date(2))
				.withComparableValue("name")
				.withEntityId(1)
				.withTypeId("username")
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
	protected Identity getMinAPIObject()
	{
		Identity ret = new Identity("username", "name", 1, "name");
		ret.setUpdateTs(new Date(2));
		ret.setCreationTs(new Date(1));
		return ret;
	}

	@Override
	protected DBIdentity getMinDBObject()
	{
		return DBIdentity.builder()
				.withCreationTs(new Date(1))
				.withUpdateTs(new Date(2))
				.withComparableValue("name")
				.withEntityId(1)
				.withTypeId("username")
				.withValue("name")
				.withConfirmationInfo(DBConfirmationInfo.builder()
						.withConfirmed(false)
						.build())
				.build();
	}

	@Override
	protected Pair<Function<Identity, DBIdentity>, Function<DBIdentity, Identity>> getMapper()
	{
		return Pair.of(IdentityMapper::map, IdentityMapper::map);
	}
}
