/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBPolicyAgreementDecisionTest extends DBTypeTestBase<DBPolicyAgreementDecision>
{
	@Override
	protected String getJson()
	{
		return "{\"documentsIdsToAccept\":[1,2],\"acceptanceStatus\":\"ACCEPTED\"}\n";
	}

	@Override
	protected DBPolicyAgreementDecision getObject()
	{
		return DBPolicyAgreementDecision.builder()
				.withAcceptanceStatus("ACCEPTED")
				.withDocumentsIdsToAccept(List.of(1L, 2L))
				.build();
	}

}
