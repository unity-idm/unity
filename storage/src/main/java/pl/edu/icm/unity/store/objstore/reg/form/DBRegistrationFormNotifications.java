/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBBaseFormNotifications;

@JsonDeserialize(builder = DBRegistrationFormNotifications.Builder.class)
class DBRegistrationFormNotifications extends DBBaseFormNotifications
{
	final String submittedTemplate;

	private DBRegistrationFormNotifications(Builder builder)
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
		DBRegistrationFormNotifications other = (DBRegistrationFormNotifications) obj;
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

		public DBRegistrationFormNotifications build()
		{
			return new DBRegistrationFormNotifications(this);
		}
	}
}
