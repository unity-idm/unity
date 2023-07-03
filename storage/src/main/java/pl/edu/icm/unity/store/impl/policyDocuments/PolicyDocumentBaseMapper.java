/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.policyDocuments;

import java.util.Optional;

import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

class PolicyDocumentBaseMapper
{
	static DBPolicyDocumentBase map(StoredPolicyDocument policyDocument)
	{
		return DBPolicyDocumentBase.builder()
				.withDisplayedName(Optional.ofNullable(policyDocument.getDisplayedName())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withMandatory(policyDocument.isMandatory())
				.withContentType(policyDocument.getContentType()
						.name())
				.withRevision(policyDocument.getRevision())
				.withContent(Optional.ofNullable(policyDocument.getContent())
						.map(I18nStringMapper::map)
						.orElse(null))
				.build();
	}

	static StoredPolicyDocument map(DBPolicyDocumentBase policyDocument, Long id, String name)
	{
		StoredPolicyDocument storedPolicyDocument = new StoredPolicyDocument(id, name);
		storedPolicyDocument.setDisplayedName(Optional.ofNullable(policyDocument.displayedName)
				.map(I18nStringMapper::map)
				.orElse(null));
		storedPolicyDocument.setContent(Optional.ofNullable(policyDocument.content)
				.map(I18nStringMapper::map)
				.orElse(null));
		storedPolicyDocument.setContentType(PolicyDocumentContentType.valueOf(policyDocument.contentType));
		storedPolicyDocument.setMandatory(policyDocument.mandatory);
		storedPolicyDocument.setRevision(policyDocument.revision);
		return storedPolicyDocument;

	}
}
