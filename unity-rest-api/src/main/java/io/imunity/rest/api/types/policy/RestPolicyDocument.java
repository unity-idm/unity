/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = RestPolicyDocument.RestPolicyDocumentBuilder.class)
public class RestPolicyDocument
{
	public final Long id;
	public final String name;
	public final Map<String, String> displayedName;
	public final boolean mandatory;
	public final String contentType;
	public final int revision;

	RestPolicyDocument(Long id, String name, Map<String, String> displayedName, boolean mandatory, String contentType,
	                   int revision)
	{
		this.id = id;
		this.name = name;
		this.displayedName = Map.copyOf(displayedName);
		this.mandatory = mandatory;
		this.contentType = contentType;
		this.revision = revision;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestPolicyDocument that = (RestPolicyDocument) o;
		return mandatory == that.mandatory && revision == that.revision && Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(displayedName, that.displayedName) && Objects.equals(contentType, that.contentType);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, displayedName, mandatory, contentType, revision);
	}

	@Override
	public String toString()
	{
		return "RestPolicyDocument{" +
			"id=" + id +
			", name='" + name + '\'' +
			", displayedName=" + displayedName +
			", mandatory=" + mandatory +
			", contentType='" + contentType + '\'' +
			", revision=" + revision +
			'}';
	}

	public static RestPolicyDocumentBuilder builder()
	{
		return new RestPolicyDocumentBuilder();
	}

	public static final class RestPolicyDocumentBuilder
	{
		private Long id;
		private String name;
		private Map<String, String> displayedName;
		private boolean mandatory;
		private String contentType;
		private int revision;

		private RestPolicyDocumentBuilder()
		{
		}

		public RestPolicyDocumentBuilder withId(Long id)
		{
			this.id = id;
			return this;
		}

		public RestPolicyDocumentBuilder withName(String name)
		{
			this.name = name;
			return this;
		}

		public RestPolicyDocumentBuilder withDisplayedName(Map<String, String> displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public RestPolicyDocumentBuilder withMandatory(boolean mandatory)
		{
			this.mandatory = mandatory;
			return this;
		}

		public RestPolicyDocumentBuilder withContentType(String contentType)
		{
			this.contentType = contentType;
			return this;
		}

		public RestPolicyDocumentBuilder withRevision(int revision)
		{
			this.revision = revision;
			return this;
		}

		public RestPolicyDocument build()
		{
			return new RestPolicyDocument(id, name, displayedName, mandatory, contentType, revision);
		}
	}
}
