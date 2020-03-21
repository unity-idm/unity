/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyDocument;

import java.util.Objects;

import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.types.I18nString;

public class PolicyDocumentCreateRequest
{
	public final String name;
	public final I18nString displayedName;
	public final boolean mandatory;
	public final PolicyDocumentContentType contentType;
	public final I18nString content;

	public PolicyDocumentCreateRequest(String name, I18nString displayedName, boolean mandatory,
			PolicyDocumentContentType contentType, I18nString content)
	{

		this.name = name;
		this.displayedName = displayedName;
		this.mandatory = mandatory;
		this.contentType = contentType;
		this.content = content;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof PolicyDocumentCreateRequest))
			return false;
		PolicyDocumentCreateRequest castOther = (PolicyDocumentCreateRequest) other;
		return Objects.equals(name, castOther.name) && Objects.equals(displayedName, castOther.displayedName)
				&& Objects.equals(content, castOther.content)
				&& Objects.equals(contentType, castOther.contentType)
				&& Objects.equals(mandatory, castOther.mandatory);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, displayedName, contentType, content, mandatory);
	}

}
