/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.enquiry;

import java.util.Objects;

public class EnquirySelector
{
	public enum Type
	{
		STICKY, REGULAR, ALL
	}

	public enum AccessMode
	{
		NON_BY_INVITATION_ONLY, ANY
	}

	public final Type type;
	public final AccessMode accessMode;

	private EnquirySelector(Builder builder)
	{
		this.type = builder.type;
		this.accessMode = builder.accessMode;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(accessMode, type);
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
		EnquirySelector other = (EnquirySelector) obj;
		return accessMode == other.accessMode && type == other.type;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Type type = Type.ALL;
		private AccessMode accessMode = AccessMode.ANY;

		private Builder()
		{
		}

		public Builder withType(Type type)
		{
			this.type = type;
			return this;
		}

		public Builder withAccessMode(AccessMode accessMode)
		{
			this.accessMode = accessMode;
			return this;
		}

		public EnquirySelector build()
		{
			return new EnquirySelector(this);
		}
	}

}
