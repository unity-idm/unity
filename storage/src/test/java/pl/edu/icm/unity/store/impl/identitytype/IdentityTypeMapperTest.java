/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

public class IdentityTypeMapperTest extends MapperTestBase<IdentityType, DBIdentityTypeBase>
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
	protected DBIdentityTypeBase getFullDBObject()
	{
		return DBIdentityTypeBase.builder()
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
				.build();
	}

	@Override
	protected Pair<Function<IdentityType, DBIdentityTypeBase>, Function<DBIdentityTypeBase, IdentityType>> getMapper()
	{
		return Pair.of(IdentityTypeBaseMapper::map, t -> IdentityTypeBaseMapper.map(t, "type"));
	}

}
