/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import java.util.Optional;

import pl.edu.icm.unity.base.entity.IdentityType;

 class IdentityTypeMapper
{
	static DBIdentityType map(IdentityType identityType)
	{
		return DBIdentityType.builder()
				.withDescription(identityType.getDescription())
				.withEmailConfirmationConfiguration(
						Optional.ofNullable(identityType.getEmailConfirmationConfiguration())
								.map(EmailConfirmationConfigurationMapper::map)
								.orElse(null))
				.withIdentityTypeProvider(identityType.getIdentityTypeProvider())
				.withIdentityTypeProviderSettings(identityType.getIdentityTypeProviderSettings())
				.withMaxInstances(identityType.getMaxInstances())
				.withMinInstances(identityType.getMinInstances())
				.withMinVerifiedInstances(identityType.getMinVerifiedInstances())
				.withSelfModificable(identityType.isSelfModificable())
				.withName(identityType.getName())
				.build();
	}

	static IdentityType map(DBIdentityType dbIdentityType)
	{
		IdentityType identityType = new IdentityType(dbIdentityType.name);
		identityType.setDescription(dbIdentityType.description);
		identityType
				.setEmailConfirmationConfiguration(Optional.ofNullable(dbIdentityType.emailConfirmationConfiguration)
						.map(EmailConfirmationConfigurationMapper::map)
						.orElse(null));
		identityType.setIdentityTypeProvider(dbIdentityType.identityTypeProvider);
		identityType.setIdentityTypeProviderSettings(dbIdentityType.identityTypeProviderSettings);
		identityType.setMaxInstances(dbIdentityType.maxInstances);
		identityType.setMinInstances(dbIdentityType.minInstances);
		identityType.setMinVerifiedInstances(dbIdentityType.minVerifiedInstances);
		identityType.setSelfModificable(dbIdentityType.selfModificable);
		return identityType;

	}
}
