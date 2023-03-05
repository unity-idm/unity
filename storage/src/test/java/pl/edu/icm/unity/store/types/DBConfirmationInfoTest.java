/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBConfirmationInfoTest extends DBTypeTestBase<DBConfirmationInfo>
{

	@Override
	protected String getJson()
	{

		return "{\"confirmed\":true,\"confirmationDate\":1667464511776,\"sentRequestAmount\":1}\n";
	}

	@Override
	protected DBConfirmationInfo getObject()
	{
		return DBConfirmationInfo.builder()
				.withSentRequestAmount(1)
				.withConfirmed(true)
				.withConfirmationDate(1667464511776L)
				.build();
	}
}
