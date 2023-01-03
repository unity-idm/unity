/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.registration.layout.RestFormLayout;

@JsonDeserialize(builder = RestEnquiryForm.Builder.class)
public class RestEnquiryForm extends RestBaseForm
{
	public final String type;
	public final List<String> targetGroups;
	public final String targetCondition;
	@JsonProperty("NotificationsConfiguration")
	public final RestEnquiryFormNotifications notificationsConfiguration;
	@JsonProperty("FormLayout")
	public final RestFormLayout layout;

	private RestEnquiryForm(Builder builder)
	{
		super(builder);
		this.type = builder.type;
		this.targetGroups = builder.targetGroups;
		this.targetCondition = builder.targetCondition;
		this.notificationsConfiguration = builder.notificationsConfiguration;
		this.layout = builder.layout;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(layout, notificationsConfiguration, targetCondition, targetGroups, type);
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
		RestEnquiryForm other = (RestEnquiryForm) obj;
		return Objects.equals(layout, other.layout)
				&& Objects.equals(notificationsConfiguration, other.notificationsConfiguration)
				&& Objects.equals(targetCondition, other.targetCondition)
				&& Objects.equals(targetGroups, other.targetGroups) && Objects.equals(type, other.type);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestBaseFormBuilder<Builder>
	{
		private String type;
		private List<String> targetGroups = Collections.emptyList();
		private String targetCondition;
		@JsonProperty("NotificationsConfiguration")
		private RestEnquiryFormNotifications notificationsConfiguration;
		@JsonProperty("FormLayout")
		private RestFormLayout layout;

		private Builder()
		{
		}

		public Builder withType(String type)
		{
			this.type = type;
			return this;
		}

		public Builder withTargetGroups(List<String> targetGroups)
		{
			this.targetGroups = targetGroups;
			return this;
		}

		public Builder withTargetCondition(String targetCondition)
		{
			this.targetCondition = targetCondition;
			return this;
		}

		public Builder withNotificationsConfiguration(RestEnquiryFormNotifications notificationsConfiguration)
		{
			this.notificationsConfiguration = notificationsConfiguration;
			return this;
		}

		public Builder withLayout(RestFormLayout layout)
		{
			this.layout = layout;
			return this;
		}

		public RestEnquiryForm build()
		{
			return new RestEnquiryForm(this);
		}
	}

}
