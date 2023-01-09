/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyDocument;

import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.types.I18nString;

import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

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

	public static PolicyDocumentUpdateRequestBuilder updateRequestBuilder()
	{
		return new PolicyDocumentUpdateRequestBuilder();
	}

	public static final class PolicyDocumentUpdateRequestBuilder
	{
		public Long id;
		public String name;
		public I18nString displayedName;
		public boolean mandatory;
		public PolicyDocumentContentType contentType;
		public I18nString content;

		private PolicyDocumentUpdateRequestBuilder()
		{
		}

		public PolicyDocumentUpdateRequestBuilder withId(Long id)
		{
			this.id = id;
			return this;
		}

		public PolicyDocumentUpdateRequestBuilder withName(String name)
		{
			this.name = name;
			return this;
		}

		public PolicyDocumentUpdateRequestBuilder withDisplayedName(Map<String, String> displayedName)
		{
			this.displayedName = new I18nString();
			this.displayedName.addAllValues(displayedName);
			ofNullable(displayedName.get("")).ifPresent(this.displayedName::setDefaultValue);
			return this;
		}

		public PolicyDocumentUpdateRequestBuilder withMandatory(boolean mandatory)
		{
			this.mandatory = mandatory;
			return this;
		}

		public PolicyDocumentUpdateRequestBuilder withContentType(String contentType)
		{
			this.contentType = PolicyDocumentContentType.valueOf(contentType);
			return this;
		}

		public PolicyDocumentUpdateRequestBuilder withContent(Map<String, String> content)
		{
			this.content = new I18nString();
			this.content.addAllValues(content);
			ofNullable(content.get("")).ifPresent(this.content::setDefaultValue);
			return this;
		}

		public PolicyDocumentUpdateRequest build()
		{
			return new PolicyDocumentUpdateRequest(id, name, displayedName, mandatory, contentType, content);
		}
	}

}
