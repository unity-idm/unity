/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.version;

import java.time.Instant;
import java.util.Objects;

public class VersionInformation
{
	public final String version;
	public final Instant buildTime;

	private VersionInformation(Builder builder)
	{
		this.version = builder.version;
		this.buildTime = builder.buildTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(buildTime, version);
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
		VersionInformation other = (VersionInformation) obj;
		return Objects.equals(buildTime, other.buildTime) && Objects.equals(version, other.version);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String version;
		private Instant buildTime;

		private Builder()
		{
		}

		public Builder withVersion(String version)
		{
			this.version = version;
			return this;
		}

		public Builder withBuildTime(Instant buildTime)
		{
			this.buildTime = buildTime;
			return this;
		}

		public VersionInformation build()
		{
			return new VersionInformation(this);
		}
	}
}
