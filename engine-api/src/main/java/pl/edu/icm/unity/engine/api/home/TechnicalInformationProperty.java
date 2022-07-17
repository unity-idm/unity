/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.home;

public class TechnicalInformationProperty
{
	public final String titleKey;
	public final String value;

	private TechnicalInformationProperty(Builder builder)
	{
		this.titleKey = builder.titleKey;
		this.value = builder.value;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String titleKey;
		private String value;

		private Builder()
		{
		}

		public Builder withTitleKey(String titleKey)
		{
			this.titleKey = titleKey;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public TechnicalInformationProperty build()
		{
			return new TechnicalInformationProperty(this);
		}
	}

}