/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;

public class RestConfirmationInfoTest extends RestTypeBase<RestConfirmationInfo>
{

	@Override
	protected String getJson()
	{

		return "{\"confirmed\":true,\"confirmationDate\":1667464511776,\"sentRequestAmount\":1}\n";
	}

	@Override
	protected RestConfirmationInfo getObject()
	{
		return RestConfirmationInfo.builder()
				.withSentRequestAmount(1)
				.withConfirmed(true)
				.withConfirmationDate(1667464511776L)
				.build();
	}
}
