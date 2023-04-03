/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types.common;

import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class ConfirmationInfoMapper
{
	public static DBConfirmationInfo map(ConfirmationInfo confirmationInfo)
	{
		return DBConfirmationInfo.builder()
				.withConfirmationDate(confirmationInfo.getConfirmationDate())
				.withConfirmed(confirmationInfo.isConfirmed())
				.withSentRequestAmount(confirmationInfo.getSentRequestAmount())
				.build();

	}

	public static ConfirmationInfo map(DBConfirmationInfo dbConfirmationInfo)
	{
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(dbConfirmationInfo.confirmed);
		confirmationInfo.setConfirmationDate(dbConfirmationInfo.confirmationDate);
		confirmationInfo.setSentRequestAmount(dbConfirmationInfo.sentRequestAmount);
		return confirmationInfo;

	}
}
