/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.policyDocuments;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

public class PolicyDocumentEntry implements FilterableEntry
{
	public final PolicyDocumentWithRevision doc;

	public PolicyDocumentEntry(PolicyDocumentWithRevision org)
	{
		this.doc = org;
	}

	@Override
	public boolean anyFieldContains(String searched, MessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (doc.name != null && doc.name.toLowerCase().contains(textLower))
			return true;

		if (String.valueOf(doc.revision).toLowerCase().contains(textLower))
			return true;

		if (doc.contentType != null && doc.contentType.toString().toLowerCase().contains(textLower))
			return true;

		return false;
	}

}
