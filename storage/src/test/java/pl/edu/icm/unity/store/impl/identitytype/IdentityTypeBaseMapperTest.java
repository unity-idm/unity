/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import java.util.function.Function;

import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class IdentityTypeBaseMapperTest extends MapperTestBase<IdentityType, DBIdentityType>
{
	@Override
	protected IdentityType getFullAPIObject()
	{
		EmailConfirmationConfiguration emailConfirmationConfiguration = new EmailConfirmationConfiguration("template");
		emailConfirmationConfiguration.setValidityTime(10);

		IdentityType identityType = new IdentityType("type");
		identityType.setEmailConfirmationConfiguration(emailConfirmationConfiguration);
		identityType.setDescription("desc");
		identityType.setIdentityTypeProvider("typeProvider");
		identityType.setIdentityTypeProviderSettings("providerSettings");
		identityType.setMaxInstances(1);
		identityType.setMinInstances(1);
		identityType.setMinVerifiedInstances(2);
		identityType.setSelfModificable(true);
		return identityType;
	}

	@Override
	protected DBIdentityType getFullDBObject()
	{
		return DBIdentityType.builder()
				.withDescription("desc")
				.withEmailConfirmationConfiguration(DBEmailConfirmationConfiguration.builder()
						.withValidityTime(10)
						.withMessageTemplate("template")
						.build())
				.withIdentityTypeProvider("typeProvider")
				.withIdentityTypeProviderSettings("providerSettings")
				.withMaxInstances(1)
				.withMinInstances(1)
				.withMinVerifiedInstances(2)
				.withSelfModificable(true)
				.withName("type")
				.build();
	}

	@Override
	protected Pair<Function<IdentityType, DBIdentityType>, Function<DBIdentityType, IdentityType>> getMapper()
	{
		return Pair.of(IdentityTypeMapper::map, IdentityTypeMapper::map);
	}

}
