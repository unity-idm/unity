/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.credreq;

import java.util.Set;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

public class CredentialRequirementsMapperTest extends MapperTestBase<CredentialRequirements, DBCredentialRequirements>
{

	@Override
	protected CredentialRequirements getFullAPIObject()
	{
		CredentialRequirements credentialRequirements = new CredentialRequirements("name", "desc", Set.of("req1"));
		credentialRequirements.setReadOnly(true);
		return credentialRequirements;
	}

	@Override
	protected DBCredentialRequirements getFullDBObject()
	{
		return DBCredentialRequirements.builder()
				.withDescription("desc")
				.withName("name")
				.withReadOnly(true)
				.withRequiredCredentials(Set.of("req1"))
				.build();
	}

	@Override
	protected Pair<Function<CredentialRequirements, DBCredentialRequirements>, Function<DBCredentialRequirements, CredentialRequirements>> getMapper()
	{
		return Pair.of(CredentialRequirementsMapper::map, CredentialRequirementsMapper::map);
	}

}
