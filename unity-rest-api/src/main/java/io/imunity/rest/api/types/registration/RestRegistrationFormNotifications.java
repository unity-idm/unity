/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestRegistrationFormNotifications.Builder.class)
public class RestRegistrationFormNotifications extends RestBaseFormNotifications
{
	public final String submittedTemplate;

	private RestRegistrationFormNotifications(Builder builder)
	{
		super(builder);
		this.submittedTemplate = builder.submittedTemplate;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(submittedTemplate);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestRegistrationFormNotifications other = (RestRegistrationFormNotifications) obj;
		return Objects.equals(submittedTemplate, other.submittedTemplate);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestBaseFormNotificationsBuilder<Builder>
	{

		private String submittedTemplate;

		private Builder()
		{
		}

		public Builder withSubmittedTemplate(String submittedTemplate)
		{
			this.submittedTemplate = submittedTemplate;
			return this;
		}

		public RestRegistrationFormNotifications build()
		{
			return new RestRegistrationFormNotifications(this);
		}
	}
}
