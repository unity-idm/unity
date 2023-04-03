/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.cred;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

public class CredentialDefinitionMapperTest extends MapperTestBase<CredentialDefinition, DBCredentialDefinition>
{

	@Override
	protected CredentialDefinition getFullAPIObject()
	{
		CredentialDefinition credentialDefinition = new CredentialDefinition("type", "name");
		credentialDefinition.setConfiguration("config");
		credentialDefinition.setDescription(new I18nString("desc"));
		credentialDefinition.setDisplayedName(new I18nString("disp"));
		credentialDefinition.setReadOnly(true);
		return credentialDefinition;
	}

	@Override
	protected DBCredentialDefinition getFullDBObject()
	{
		return DBCredentialDefinition.builder()
				.withConfiguration("config")
				.withI18nDescription(DBI18nString.builder()
						.withDefaultValue("desc")
						.build())
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("disp")
						.build())
				.withName("name")
				.withTypeId("type")
				.withReadOnly(true)
				.build();
	}

	@Override
	protected Pair<Function<CredentialDefinition, DBCredentialDefinition>, Function<DBCredentialDefinition, CredentialDefinition>> getMapper()
	{
		return Pair.of(CredentialDefinitionMapper::map, CredentialDefinitionMapper::map);
	}

}
