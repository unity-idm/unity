/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eform;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBBaseFormNotifications;

@JsonDeserialize(builder = DBEnquiryFormNotifications.Builder.class)
class DBEnquiryFormNotifications extends DBBaseFormNotifications
{
	public final String enquiryToFillTemplate;
	public final String submittedTemplate;

	private DBEnquiryFormNotifications(Builder builder)
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
		DBEnquiryFormNotifications other = (DBEnquiryFormNotifications) obj;
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

		public DBEnquiryFormNotifications build()
		{
			return new DBEnquiryFormNotifications(this);
		}
	}
}
