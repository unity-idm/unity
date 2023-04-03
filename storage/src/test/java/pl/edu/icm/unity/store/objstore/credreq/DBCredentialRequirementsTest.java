/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.credreq;

import java.util.Set;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBCredentialRequirementsTest extends DBTypeTestBase<DBCredentialRequirements>
{

	@Override
	protected String getJson()
	{
		return "{\"name\":\"name\",\"description\":\"desc\",\"requiredCredentials\":[\"req1\"],\"readOnly\":true}\n";
	}

	@Override
	protected DBCredentialRequirements getObject()
	{
		return DBCredentialRequirements.builder()
				.withDescription("desc")
				.withName("name")
				.withReadOnly(true)
				.withRequiredCredentials(Set.of("req1"))
				.build();
	}

}
