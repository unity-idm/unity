/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBGroupRegistrationParam.Builder.class)
class DBGroupRegistrationParam extends DBRegistrationParam
{

	final String groupPath;
	final boolean multiSelect;
	final String includeGroupsMode;

	private DBGroupRegistrationParam(Builder builder)
	{
		super(builder);

		this.groupPath = builder.groupPath;
		this.includeGroupsMode = builder.includeGroupsMode;
		this.multiSelect = builder.multiSelect;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(groupPath, includeGroupsMode, multiSelect);
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
		DBGroupRegistrationParam other = (DBGroupRegistrationParam) obj;
		return Objects.equals(groupPath, other.groupPath) && Objects.equals(includeGroupsMode, other.includeGroupsMode)
				&& multiSelect == other.multiSelect;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestRegistrationParamBuilder<Builder>
	{
		private String groupPath;
		private boolean multiSelect;
		private String includeGroupsMode;

		private Builder()
		{
		}

		public Builder withGroupPath(String groupPath)
		{
			this.groupPath = groupPath;
			return this;
		}

		public Builder withMultiSelect(boolean multiSelect)
		{
			this.multiSelect = multiSelect;
			return this;
		}

		public Builder withIncludeGroupsMode(String includeGroupsMode)
		{
			this.includeGroupsMode = includeGroupsMode;
			return this;
		}

		public DBGroupRegistrationParam build()
		{
			return new DBGroupRegistrationParam(this);
		}
	}

}
