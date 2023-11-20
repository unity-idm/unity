/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = RestPolicyDocumentUpdateRequest.RestPolicyDocumentUpdateRequestBuilder.class)
public class RestPolicyDocumentUpdateRequest
{
	public final Long id;
	public final String name;
	public final Map<String, String> displayedName;
	public final Boolean mandatory;
	public final String contentType;
	public final Map<String, String> content;

	public RestPolicyDocumentUpdateRequest(Long id, String name, Map<String, String> displayedName, Boolean mandatory,
	                                 String contentType, Map<String, String> content)
	{

		this.id = id;
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
		RestPolicyDocumentUpdateRequest that = (RestPolicyDocumentUpdateRequest) o;
		return Objects.equals(id, that.id) && mandatory == that.mandatory && Objects.equals(name, that.name) && Objects.equals(displayedName,
			that.displayedName) && Objects.equals(contentType, that.contentType) && Objects.equals(content, that.content);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, displayedName, contentType, content, mandatory);
	}

	@Override
	public String toString()
	{
		return "RestPolicyDocumentUpdateRequest{" +
			"id='" + id + '\'' +
			"name='" + name + '\'' +
			", displayedName=" + displayedName +
			", mandatory=" + mandatory +
			", contentType='" + contentType + '\'' +
			", content=" + content +
			'}';
	}

	public static RestPolicyDocumentUpdateRequestBuilder builder()
	{
		return new RestPolicyDocumentUpdateRequestBuilder();
	}

	public static final class RestPolicyDocumentUpdateRequestBuilder
	{
		private Long id;
		private String name;
		private Map<String, String> displayedName;
		private Boolean mandatory;
		private String contentType;
		private Map<String, String> content;

		private RestPolicyDocumentUpdateRequestBuilder()
		{
		}

		public RestPolicyDocumentUpdateRequestBuilder withId(Long id)
		{
			this.id = id;
			return this;
		}
		
		public RestPolicyDocumentUpdateRequestBuilder withName(String name)
		{
			this.name = name;
			return this;
		}

		public RestPolicyDocumentUpdateRequestBuilder withDisplayedName(Map<String, String> displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public RestPolicyDocumentUpdateRequestBuilder withMandatory(Boolean mandatory)
		{
			this.mandatory = mandatory;
			return this;
		}

		public RestPolicyDocumentUpdateRequestBuilder withContentType(String contentType)
		{
			this.contentType = contentType;
			return this;
		}

		public RestPolicyDocumentUpdateRequestBuilder withContent(Map<String, String> content)
		{
			this.content = content;
			return this;
		}

		public RestPolicyDocumentUpdateRequest build()
		{
			return new RestPolicyDocumentUpdateRequest(id, name, displayedName, mandatory, contentType, content);
		}
	}
}
