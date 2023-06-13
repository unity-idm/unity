/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;

public class ConfirmationInfoMapper
{
	static RestConfirmationInfo map(ConfirmationInfo confirmationInfo)
	{
		return RestConfirmationInfo.builder()
				.withConfirmationDate(confirmationInfo.getConfirmationDate())
				.withConfirmed(confirmationInfo.isConfirmed())
				.withSentRequestAmount(confirmationInfo.getSentRequestAmount())
				.build();

	}

	static ConfirmationInfo map(RestConfirmationInfo restConfirmationInfo)
	{
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(restConfirmationInfo.confirmed);
		confirmationInfo.setConfirmationDate(restConfirmationInfo.confirmationDate);
		confirmationInfo.setSentRequestAmount(restConfirmationInfo.sentRequestAmount);
		return confirmationInfo;

	}
}
