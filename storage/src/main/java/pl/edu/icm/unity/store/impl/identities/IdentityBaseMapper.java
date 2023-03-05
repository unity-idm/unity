/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identities;

import java.util.Optional;

import pl.edu.icm.unity.store.types.ConfirmationInfoMapper;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class IdentityBaseMapper
{
	public static DBIdentityBase map(Identity identity)
	{
		return DBIdentityBase.builder()
				.withCreationTs(identity.getCreationTs())
				.withUpdateTs(identity.getUpdateTs())
				.withComparableValue(identity.getComparableValue())
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

	static Identity map(DBIdentityBase dbIdentity, String typeId, long entityId)
	{
		Identity identity = new Identity(typeId, dbIdentity.value, entityId,
				dbIdentity.comparableValue);
		identity.setConfirmationInfo(Optional.ofNullable(dbIdentity.confirmationInfo)
				.map(ConfirmationInfoMapper::map)
				.orElse(new ConfirmationInfo(false)));
		identity.setCreationTs(dbIdentity.creationTs);
		identity.setMetadata(dbIdentity.metadata);
		identity.setRealm(dbIdentity.realm);
		identity.setTarget(dbIdentity.target);
		identity.setRemoteIdp(dbIdentity.remoteIdp);
		identity.setTranslationProfile(dbIdentity.translationProfile);
		identity.setUpdateTs(dbIdentity.updateTs);
		return identity;
	}

}
