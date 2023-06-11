/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import java.util.function.Function;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.ConfirmationInfoMapper;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;

public class ConfirmationInfoMapperTest extends MapperTestBase<ConfirmationInfo, DBConfirmationInfo>
{

	@Override
	protected ConfirmationInfo getFullAPIObject()
	{
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(true);
		confirmationInfo.setConfirmationDate(99);
		confirmationInfo.setSentRequestAmount(1);
		return confirmationInfo;
	}

	@Override
	protected DBConfirmationInfo getFullDBObject()
	{
		return DBConfirmationInfo.builder()
				.withConfirmationDate(99)
				.withConfirmed(true)
				.withSentRequestAmount(1)
				.build();
	}

	@Override
	protected Pair<Function<ConfirmationInfo, DBConfirmationInfo>, Function<DBConfirmationInfo, ConfirmationInfo>> getMapper()
	{
		return Pair.of(ConfirmationInfoMapper::map, ConfirmationInfoMapper::map);
	}

}
