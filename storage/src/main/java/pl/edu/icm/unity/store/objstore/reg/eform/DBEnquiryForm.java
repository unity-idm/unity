/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eform;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBBaseForm;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLayout;

@JsonDeserialize(builder = DBEnquiryForm.Builder.class)
class DBEnquiryForm extends DBBaseForm
{
	public final String type;
	public final List<String> targetGroups;
	public final String targetCondition;
	@JsonProperty("NotificationsConfiguration")
	public final DBEnquiryFormNotifications notificationsConfiguration;
	@JsonProperty("FormLayout")
	public final DBFormLayout layout;

	private DBEnquiryForm(Builder builder)
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
		DBEnquiryForm other = (DBEnquiryForm) obj;
		return Objects.equals(layout, other.layout)
				&& Objects.equals(notificationsConfiguration, other.notificationsConfiguration)
				&& Objects.equals(targetCondition, other.targetCondition)
				&& Objects.equals(targetGroups, other.targetGroups) && Objects.equals(type, other.type);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends DBBaseFormBuilder<Builder>
	{
		private String type;
		private List<String> targetGroups = Collections.emptyList();
		private String targetCondition;
		@JsonProperty("NotificationsConfiguration")
		private DBEnquiryFormNotifications notificationsConfiguration;
		@JsonProperty("FormLayout")
		private DBFormLayout layout;

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

		public Builder withNotificationsConfiguration(DBEnquiryFormNotifications notificationsConfiguration)
		{
			this.notificationsConfiguration = notificationsConfiguration;
			return this;
		}

		public Builder withLayout(DBFormLayout layout)
		{
			this.layout = layout;
			return this;
		}

		public DBEnquiryForm build()
		{
			return new DBEnquiryForm(this);
		}
	}

}
