/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;

public class ConfirmationInfoMapper
{
	static RestConfirmationInfo map(ConfirmationInfo confirmationInfo)
	{
		return RestConfirmationInfo.builder().withConfirmationDate(confirmationInfo.getConfirmationDate())
				.withConfirmed(confirmationInfo.isConfirmed())
				.withSentRequestAmount(confirmationInfo.getSentRequestAmount())
				.build();

	}
}
