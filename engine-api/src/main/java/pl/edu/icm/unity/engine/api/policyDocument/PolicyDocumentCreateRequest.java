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
		this.displayedName = displayedName.clone();
		this.mandatory = mandatory;
		this.contentType = contentType;
		this.content = content.clone();
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

	public static PolicyDocumentCreateRequestBuilder createRequestBuilder()
	{
		return new PolicyDocumentCreateRequestBuilder();
	}

	public static final class PolicyDocumentCreateRequestBuilder
	{
		private String name;
		private I18nString displayedName;
		private boolean mandatory;
		private PolicyDocumentContentType contentType;
		private I18nString content;

		private PolicyDocumentCreateRequestBuilder()
		{
		}

		public PolicyDocumentCreateRequestBuilder withName(String name)
		{
			this.name = name;
			return this;
		}

		public PolicyDocumentCreateRequestBuilder withDisplayedName(Map<String, String> displayedName)
		{
			this.displayedName = new I18nString();
			this.displayedName.addAllValues(displayedName);
			ofNullable(displayedName.get("")).ifPresent(this.displayedName::setDefaultValue);
			return this;
		}

		public PolicyDocumentCreateRequestBuilder withMandatory(boolean mandatory)
		{
			this.mandatory = mandatory;
			return this;
		}

		public PolicyDocumentCreateRequestBuilder withContentType(String contentType)
		{
			this.contentType = PolicyDocumentContentType.valueOf(contentType);
			return this;
		}

		public PolicyDocumentCreateRequestBuilder withContent(Map<String, String> content)
		{
			this.content = new I18nString();
			this.content.addAllValues(content);
			ofNullable(content.get("")).ifPresent(this.content::setDefaultValue);
			return this;
		}

		public PolicyDocumentCreateRequest build()
		{
			return new PolicyDocumentCreateRequest(name, displayedName, mandatory, contentType, content);
		}
	}
}
