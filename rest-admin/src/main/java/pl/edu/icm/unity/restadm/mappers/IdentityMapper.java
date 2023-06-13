/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Optional;

import io.imunity.rest.api.types.basic.RestIdentity;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.identity.Identity;

public class IdentityMapper
{
	public static RestIdentity map(Identity identity)
	{
		return RestIdentity.builder()
				.withEntityId(identity.getEntityId())
				.withCreationTs(identity.getCreationTs())
				.withUpdateTs(identity.getUpdateTs())
				.withComparableValue(identity.getComparableValue())
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

	static Identity map(RestIdentity restIdentity)
	{
		Identity identity = new Identity(restIdentity.typeId, restIdentity.value, restIdentity.entityId,
				restIdentity.comparableValue);
		identity.setConfirmationInfo(Optional.ofNullable(restIdentity.confirmationInfo)
				.map(ConfirmationInfoMapper::map)
				.orElse(new ConfirmationInfo(false)));
		identity.setCreationTs(restIdentity.creationTs);
		identity.setMetadata(restIdentity.metadata);
		identity.setRealm(restIdentity.realm);
		identity.setTarget(restIdentity.target);
		identity.setRemoteIdp(restIdentity.remoteIdp);
		identity.setTranslationProfile(restIdentity.translationProfile);
		identity.setUpdateTs(restIdentity.updateTs);
		return identity;
	}

}
