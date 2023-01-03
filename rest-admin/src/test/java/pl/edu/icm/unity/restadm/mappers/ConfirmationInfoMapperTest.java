/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class ConfirmationInfoMapperTest extends MapperTestBase<ConfirmationInfo, RestConfirmationInfo>
{

	@Override
	protected ConfirmationInfo getAPIObject()
	{
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(true);
		confirmationInfo.setConfirmationDate(99);
		confirmationInfo.setSentRequestAmount(1);
		return confirmationInfo;
	}

	@Override
	protected RestConfirmationInfo getRestObject()
	{
		return RestConfirmationInfo.builder()
				.withConfirmationDate(99)
				.withConfirmed(true)
				.withSentRequestAmount(1)
				.build();
	}

	@Override
	protected Pair<Function<ConfirmationInfo, RestConfirmationInfo>, Function<RestConfirmationInfo, ConfirmationInfo>> getMapper()
	{
		return Pair.of(ConfirmationInfoMapper::map, ConfirmationInfoMapper::map);
	}

}
