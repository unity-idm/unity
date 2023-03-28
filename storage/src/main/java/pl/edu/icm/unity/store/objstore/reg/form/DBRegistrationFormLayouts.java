/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLayout;

@JsonDeserialize(builder = DBRegistrationFormLayouts.Builder.class)
class DBRegistrationFormLayouts
{
	public final DBFormLayout primaryLayout;
	public final DBFormLayout secondaryLayout;
	public final boolean localSignupEmbeddedAsButton;

	private DBRegistrationFormLayouts(Builder builder)
	{
		this.primaryLayout = builder.primaryLayout;
		this.secondaryLayout = builder.secondaryLayout;
		this.localSignupEmbeddedAsButton = builder.localSignupEmbeddedAsButton;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(localSignupEmbeddedAsButton, primaryLayout, secondaryLayout);
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
		DBRegistrationFormLayouts other = (DBRegistrationFormLayouts) obj;
		return localSignupEmbeddedAsButton == other.localSignupEmbeddedAsButton
				&& Objects.equals(primaryLayout, other.primaryLayout)
				&& Objects.equals(secondaryLayout, other.secondaryLayout);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private DBFormLayout primaryLayout;
		private DBFormLayout secondaryLayout;
		private boolean localSignupEmbeddedAsButton;

		private Builder()
		{
		}

		public Builder withPrimaryLayout(DBFormLayout primaryLayout)
		{
			this.primaryLayout = primaryLayout;
			return this;
		}

		public Builder withSecondaryLayout(DBFormLayout secondaryLayout)
		{
			this.secondaryLayout = secondaryLayout;
			return this;
		}

		public Builder withLocalSignupEmbeddedAsButton(boolean localSignupEmbeddedAsButton)
		{
			this.localSignupEmbeddedAsButton = localSignupEmbeddedAsButton;
			return this;
		}

		public DBRegistrationFormLayouts build()
		{
			return new DBRegistrationFormLayouts(this);
		}
	}

}
