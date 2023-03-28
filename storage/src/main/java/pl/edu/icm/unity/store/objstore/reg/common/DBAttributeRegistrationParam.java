/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAttributeRegistrationParam.Builder.class)
public class DBAttributeRegistrationParam extends DBRegistrationParam
{
	public final String attributeType;
	public final String group;
	public final boolean showGroups;
	public final boolean useDescription;
	public final String confirmationMode;
	public final DBurlQueryPrefillConfig urlQueryPrefill;
	public final boolean optional;

	private DBAttributeRegistrationParam(Builder builder)
	{
		super(builder);

		this.attributeType = builder.attributeType;
		this.group = builder.group;
		this.showGroups = builder.showGroups;
		this.useDescription = builder.useDescription;
		this.confirmationMode = builder.confirmationMode;
		this.urlQueryPrefill = builder.urlQueryPrefill;
		this.optional = builder.optional;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(attributeType, confirmationMode, group, optional, showGroups,
				urlQueryPrefill, useDescription);
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
		DBAttributeRegistrationParam other = (DBAttributeRegistrationParam) obj;
		return Objects.equals(attributeType, other.attributeType)
				&& Objects.equals(confirmationMode, other.confirmationMode) && Objects.equals(group, other.group)
				&& optional == other.optional && showGroups == other.showGroups
				&& Objects.equals(urlQueryPrefill, other.urlQueryPrefill) && useDescription == other.useDescription;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestRegistrationParamBuilder<Builder>
	{
		private String attributeType;
		private String group;
		private boolean showGroups;
		private boolean useDescription;
		private String confirmationMode;
		private DBurlQueryPrefillConfig urlQueryPrefill;
		private boolean optional;

		private Builder()
		{
		}

		public Builder withAttributeType(String attributeType)
		{
			this.attributeType = attributeType;
			return this;
		}

		public Builder withGroup(String group)
		{
			this.group = group;
			return this;
		}

		public Builder withShowGroups(boolean showGroups)
		{
			this.showGroups = showGroups;
			return this;
		}

		public Builder withUseDescription(boolean useDescription)
		{
			this.useDescription = useDescription;
			return this;
		}

		public Builder withConfirmationMode(String confirmationMode)
		{
			this.confirmationMode = confirmationMode;
			return this;
		}

		public Builder withUrlQueryPrefill(DBurlQueryPrefillConfig urlQueryPrefill)
		{
			this.urlQueryPrefill = urlQueryPrefill;
			return this;
		}

		public Builder withOptional(boolean optional)
		{
			this.optional = optional;
			return this;
		}

		public DBAttributeRegistrationParam build()
		{
			return new DBAttributeRegistrationParam(this);
		}
	}

}
