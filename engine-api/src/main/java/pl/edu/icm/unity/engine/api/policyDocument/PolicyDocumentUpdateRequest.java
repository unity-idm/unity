/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyDocument;

import java.util.Objects;

import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.types.I18nString;

public class PolicyDocumentUpdateRequest extends PolicyDocumentCreateRequest
{
	public final Long id;

	public PolicyDocumentUpdateRequest(Long id, String name, I18nString displayedName, boolean mandatory,
			PolicyDocumentContentType contentType, I18nString content)
	{
		super(name, displayedName, mandatory, contentType, content);
		this.id = id;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof PolicyDocumentUpdateRequest))
			return false;
		PolicyDocumentUpdateRequest castOther = (PolicyDocumentUpdateRequest) other;
		return Objects.equals(id, castOther.id) && Objects.equals(name, castOther.name)
				&& Objects.equals(displayedName, castOther.displayedName)
				&& Objects.equals(content, castOther.content)
				&& Objects.equals(contentType, castOther.contentType)
				&& Objects.equals(mandatory, castOther.mandatory);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, displayedName, contentType, content, mandatory);
	}

}
