/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Optional;

import io.imunity.rest.api.types.basic.RestIdentityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class IdentityParamMapper
{
	public static RestIdentityParam map(IdentityParam identity)
	{
		return RestIdentityParam.builder()
				.withTypeId(identity.getTypeId())
				.withValue(identity.getValue())
				.withTarget(identity.getTarget())
				.withRealm(identity.getRealm())
				.withRemoteIdp(identity.getRemoteIdp())
				.withConfirmationInfo(Optional.ofNullable(identity.getConfirmationInfo())
						.map(ConfirmationInfoMapper::map)
						.orElse(null))
				.withMetadata(identity.getMetadata())
				.withTranslationProfile(identity.getTranslationProfile())
				.build();
	}

	public static IdentityParam map(RestIdentityParam restIdentity)
	{
		IdentityParam identity = new IdentityParam(restIdentity.typeId, restIdentity.value, restIdentity.remoteIdp,
				restIdentity.translationProfile);
		identity.setConfirmationInfo(Optional.ofNullable(restIdentity.confirmationInfo)
				.map(ConfirmationInfoMapper::map)
				.orElse(new ConfirmationInfo(false)));
		identity.setMetadata(restIdentity.metadata);
		identity.setRealm(restIdentity.realm);
		identity.setTarget(restIdentity.target);
		return identity;
	}

}
