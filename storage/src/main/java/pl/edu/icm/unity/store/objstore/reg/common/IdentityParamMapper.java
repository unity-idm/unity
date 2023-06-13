/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.store.types.common.ConfirmationInfoMapper;

public class IdentityParamMapper
{
	public static DBIdentityParam map(IdentityParam identity)
	{
		return DBIdentityParam.builder()
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

	public static IdentityParam map(DBIdentityParam restIdentity)
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
