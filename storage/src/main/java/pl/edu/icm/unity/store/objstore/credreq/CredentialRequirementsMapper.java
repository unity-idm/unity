/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.credreq;

import pl.edu.icm.unity.types.authn.CredentialRequirements;

class CredentialRequirementsMapper
{
	static DBCredentialRequirements map(CredentialRequirements credentialRequirements)
	{
		return DBCredentialRequirements.builder()
				.withDescription(credentialRequirements.getDescription())
				.withName(credentialRequirements.getName())
				.withReadOnly(credentialRequirements.isReadOnly())
				.withRequiredCredentials(credentialRequirements.getRequiredCredentials())
				.build();
	}

	static CredentialRequirements map(DBCredentialRequirements dbCredentialRequirement)
	{
		CredentialRequirements credentialRequirements = new CredentialRequirements(dbCredentialRequirement.name,
				dbCredentialRequirement.description, dbCredentialRequirement.requiredCredentials);
		credentialRequirements.setReadOnly(dbCredentialRequirement.readOnly);
		return credentialRequirements;
	}

}
