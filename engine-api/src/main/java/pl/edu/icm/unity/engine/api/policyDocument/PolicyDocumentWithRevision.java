/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyDocument;

import java.util.Objects;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;

public class PolicyDocumentWithRevision
{
	public final Long id;
	public final String name;
	public final I18nString displayedName;
	public final boolean mandatory;
	public final PolicyDocumentContentType contentType;
	public final I18nString content;
	public final int revision;

	public PolicyDocumentWithRevision(Long id, String name, I18nString displayedName, boolean mandatory,
			PolicyDocumentContentType contentType, I18nString content, int revision)
	{

		this.id = id;
		this.name = name;
		this.displayedName = displayedName;
		this.mandatory = mandatory;
		this.contentType = contentType;
		this.content = content;
		this.revision = revision;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof PolicyDocumentWithRevision))
			return false;
		PolicyDocumentWithRevision castOther = (PolicyDocumentWithRevision) other;
		return Objects.equals(id, castOther.id) && Objects.equals(name, castOther.name)
				&& Objects.equals(displayedName, castOther.displayedName)
				&& Objects.equals(content, castOther.content)
				&& Objects.equals(contentType, castOther.contentType)
				&& Objects.equals(mandatory, castOther.mandatory)
				&& Objects.equals(revision, castOther.revision);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, displayedName, contentType, content, mandatory, revision);
	}
}
