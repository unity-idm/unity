/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = RestPolicyDocumentRequest.RestPolicyDocumentCreateRequestBuilder.class)
public class RestPolicyDocumentRequest
{
	public final String name;
	public final Map<String, String> displayedName;
	public final Boolean mandatory;
	public final String contentType;
	public final Map<String, String> content;

	public RestPolicyDocumentRequest(String name, Map<String, String> displayedName, Boolean mandatory,
	                                 String contentType, Map<String, String> content)
	{

		this.name = name;
		this.displayedName = displayedName == null ? null : Map.copyOf(displayedName);
		this.mandatory = mandatory;
		this.contentType = contentType;
		this.content = content == null ? null : Map.copyOf(content);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestPolicyDocumentRequest that = (RestPolicyDocumentRequest) o;
		return mandatory == that.mandatory && Objects.equals(name, that.name) && Objects.equals(displayedName,
			that.displayedName) && Objects.equals(contentType, that.contentType) && Objects.equals(content, that.content);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, displayedName, contentType, content, mandatory);
	}

	@Override
	public String toString()
	{
		return "RestPolicyDocumentCreateRequest{" +
			"name='" + name + '\'' +
			", displayedName=" + displayedName +
			", mandatory=" + mandatory +
			", contentType='" + contentType + '\'' +
			", content=" + content +
			'}';
	}

	public static RestPolicyDocumentCreateRequestBuilder builder()
	{
		return new RestPolicyDocumentCreateRequestBuilder();
	}

	public static final class RestPolicyDocumentCreateRequestBuilder
	{
		private String name;
		private Map<String, String> displayedName;
		private Boolean mandatory;
		private String contentType;
		private Map<String, String> content;

		private RestPolicyDocumentCreateRequestBuilder()
		{
		}

		public RestPolicyDocumentCreateRequestBuilder withName(String name)
		{
			this.name = name;
			return this;
		}

		public RestPolicyDocumentCreateRequestBuilder withDisplayedName(Map<String, String> displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public RestPolicyDocumentCreateRequestBuilder withMandatory(Boolean mandatory)
		{
			this.mandatory = mandatory;
			return this;
		}

		public RestPolicyDocumentCreateRequestBuilder withContentType(String contentType)
		{
			this.contentType = contentType;
			return this;
		}

		public RestPolicyDocumentCreateRequestBuilder withContent(Map<String, String> content)
		{
			this.content = content;
			return this;
		}

		public RestPolicyDocumentRequest build()
		{
			return new RestPolicyDocumentRequest(name, displayedName, mandatory, contentType, content);
		}
	}
}
