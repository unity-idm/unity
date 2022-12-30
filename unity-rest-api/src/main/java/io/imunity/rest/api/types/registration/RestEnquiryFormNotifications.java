/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEnquiryFormNotifications.Builder.class)
public class RestEnquiryFormNotifications extends RestBaseFormNotifications
{
	public final String enquiryToFillTemplate;
	public final String submittedTemplate;

	private RestEnquiryFormNotifications(Builder builder)
	{
		super(builder);
		this.submittedTemplate = builder.submittedTemplate;
		this.enquiryToFillTemplate = builder.enquiryToFillTemplate;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(enquiryToFillTemplate, submittedTemplate);
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
		RestEnquiryFormNotifications other = (RestEnquiryFormNotifications) obj;
		return Objects.equals(enquiryToFillTemplate, other.enquiryToFillTemplate)
				&& Objects.equals(submittedTemplate, other.submittedTemplate);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestBaseFormNotificationsBuilder<Builder>
	{

		private String submittedTemplate;
		private String enquiryToFillTemplate;

		private Builder()
		{
		}

		public Builder withSubmittedTemplate(String submittedTemplate)
		{
			this.submittedTemplate = submittedTemplate;
			return this;
		}

		public Builder withEnquiryToFillTemplate(String enquiryToFillTemplate)
		{
			this.enquiryToFillTemplate = enquiryToFillTemplate;
			return this;
		}

		public RestEnquiryFormNotifications build()
		{
			return new RestEnquiryFormNotifications(this);
		}
	}
}
