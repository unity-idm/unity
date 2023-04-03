/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;


import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class IdentityParamMapperTest extends MapperWithMinimalTestBase<IdentityParam, DBIdentityParam>
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
	protected DBIdentityParam getFullDBObject()
	{
		return DBIdentityParam.builder()
				.withValue("test@wp.pl")
				.withTypeId("email")
				.withRealm("realm")
				.withRemoteIdp("remoteIdp")
				.withTarget("target")
				.withMetadata(Constants.MAPPER.createObjectNode())
				.withTranslationProfile("Profile")
				.withConfirmationInfo(DBConfirmationInfo.builder()
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
	protected DBIdentityParam getMinDBObject()
	{
		return DBIdentityParam.builder()
				.withValue("test@wp.pl")
				.withTypeId("email")
				.build();
	}

	@Override
	protected Pair<Function<IdentityParam, DBIdentityParam>, Function<DBIdentityParam, IdentityParam>> getMapper()
	{

		return Pair.of(IdentityParamMapper::map, IdentityParamMapper::map);

	}
}
