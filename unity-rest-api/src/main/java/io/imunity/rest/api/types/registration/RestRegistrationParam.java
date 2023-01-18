/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

public class RestRegistrationParam
{
	public final String label;
	public final String description;
	public final String retrievalSettings;

	public RestRegistrationParam(RestRegistrationParamBuilder<?> builder)
	{
		this.label = builder.label;
		this.description = builder.description;
		this.retrievalSettings = builder.retrievalSettings;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, label, retrievalSettings);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestRegistrationParam other = (RestRegistrationParam) obj;
		return Objects.equals(description, other.description) && Objects.equals(label, other.label)
				&& Objects.equals(retrievalSettings, other.retrievalSettings);
	}

	public static class RestRegistrationParamBuilder<T extends RestRegistrationParamBuilder<?>>
	{
		private String label;
		private String description;
		private String retrievalSettings;

		public RestRegistrationParamBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withLabel(String label)
		{
			this.label = label;
			return (T) this;

		}

		@SuppressWarnings("unchecked")
		public T withDescription(String description)
		{
			this.description = description;
			return (T) this;

		}

		@SuppressWarnings("unchecked")
		public T withRetrievalSettings(String retrievalSettings)
		{
			this.retrievalSettings = retrievalSettings;
			return (T) this;
		}

		public RestRegistrationParam build()
		{
			return new RestRegistrationParam(this);
		}
	}

}
