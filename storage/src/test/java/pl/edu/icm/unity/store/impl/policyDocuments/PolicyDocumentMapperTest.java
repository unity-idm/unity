/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.policyDocuments;

import java.util.function.Function;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class PolicyDocumentMapperTest extends MapperTestBase<StoredPolicyDocument, DBPolicyDocument>
{

	@Override
	protected StoredPolicyDocument getFullAPIObject()
	{
		StoredPolicyDocument storedPolicyDocument = new StoredPolicyDocument(1L, "name");
		storedPolicyDocument.setContent(new I18nString("content"));
		storedPolicyDocument.setContentType(PolicyDocumentContentType.EMBEDDED);
		storedPolicyDocument.setDisplayedName(new I18nString("dispName"));
		storedPolicyDocument.setMandatory(true);
		storedPolicyDocument.setRevision(1);
		return storedPolicyDocument;
	}

	@Override
	protected DBPolicyDocument getFullDBObject()
	{

		return DBPolicyDocument.builder()
				.withId(1L)
				.withName("name")
				.withContent(DBI18nString.builder()
						.withDefaultValue("content")
						.build())
				.withContentType("EMBEDDED")
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("dispName")
						.build())
				.withMandatory(true)
				.withRevision(1)
				.build();

	}

	@Override
	protected Pair<Function<StoredPolicyDocument, DBPolicyDocument>, Function<DBPolicyDocument, StoredPolicyDocument>> getMapper()
	{
		return Pair.of(PolicyDocumentMapper::map, PolicyDocumentMapper::map);
	}

}
