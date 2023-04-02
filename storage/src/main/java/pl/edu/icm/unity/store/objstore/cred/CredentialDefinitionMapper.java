/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.cred;

import java.util.Optional;

import pl.edu.icm.unity.store.types.I18nStringMapper;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

class CredentialDefinitionMapper
{
	static DBCredentialDefinition map(CredentialDefinition credentialDefinition)
	{
		return DBCredentialDefinition.builder()
				.withConfiguration(credentialDefinition.getConfiguration())
				.withI18nDescription(Optional.ofNullable(credentialDefinition.getDescription())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withDisplayedName(Optional.ofNullable(credentialDefinition.getDisplayedName())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withName(credentialDefinition.getName())
				.withReadOnly(credentialDefinition.isReadOnly())
				.withTypeId(credentialDefinition.getTypeId())
				.build();
	}

	static CredentialDefinition map(DBCredentialDefinition dbCredentialDefinition)
	{
		CredentialDefinition credentialDefinition = new CredentialDefinition(dbCredentialDefinition.typeId,
				dbCredentialDefinition.name);
		credentialDefinition.setConfiguration(dbCredentialDefinition.configuration);
		credentialDefinition.setDescription(Optional.ofNullable(dbCredentialDefinition.i18nDescription)
				.map(I18nStringMapper::map)
				.orElse(null));
		credentialDefinition.setDisplayedName(Optional.ofNullable(dbCredentialDefinition.displayedName)
				.map(I18nStringMapper::map)
				.orElse(new I18nString(dbCredentialDefinition.name)));
		credentialDefinition.setReadOnly(dbCredentialDefinition.readOnly);

		return credentialDefinition;
	}

}
