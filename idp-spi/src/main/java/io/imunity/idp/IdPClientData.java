/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.idp;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class IdPClientData
{
	public enum AccessStatus
	{
		allow, allowWithoutAsking, disallowWithoutAsking
	}

	public final ApplicationId applicationId;
	public final Optional<String> applicationName;
	public final AccessStatus accessStatus;
	public final AccessProtocol accessProtocol;
	public final Optional<List<String>> accessScopes;
	public final Optional<Instant> accessGrantTime;
	public final Optional<Instant> accessDeniedTime;
	public final Optional<Instant> lastAccessTime;
	public final Optional<String> applicationDomain;
	public final Optional<byte[]> logo;

	public final List<TechnicalInformationProperty> technicalInformations;

	private IdPClientData(Builder builder)
	{
		this.applicationId = builder.applicationId;
		this.applicationName = builder.applicationName;
		this.accessStatus = builder.accessStatus;
		this.accessProtocol = builder.accessProtocol;
		this.accessScopes = builder.accessScopes;
		this.accessGrantTime = builder.accessGrantTime;
		this.accessDeniedTime = builder.accessDeniedTime;
		this.lastAccessTime = builder.lastAccessTime;
		this.applicationDomain = builder.applicationDomain;
		this.logo = builder.logo;
		this.technicalInformations = builder.technicalInformations;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private ApplicationId applicationId;
		private Optional<String> applicationName;
		private AccessStatus accessStatus;
		private AccessProtocol accessProtocol;
		private Optional<List<String>> accessScopes = Optional.empty();
		private Optional<Instant> accessGrantTime = Optional.empty();
		private Optional<Instant> accessDeniedTime = Optional.empty();
		private Optional<Instant> lastAccessTime = Optional.empty();
		private Optional<String> applicationDomain = Optional.empty();
		private Optional<byte[]> logo = Optional.empty();
		private List<TechnicalInformationProperty> technicalInformations = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withApplicationId(ApplicationId applicationId)
		{
			this.applicationId = applicationId;
			return this;
		}

		public Builder withApplicationName(Optional<String> applicationName)
		{
			this.applicationName = applicationName;
			return this;
		}

		public Builder withAccessStatus(AccessStatus accessStatus)
		{
			this.accessStatus = accessStatus;
			return this;
		}

		public Builder withAccessProtocol(AccessProtocol accessProtocol)
		{
			this.accessProtocol = accessProtocol;
			return this;
		}

		public Builder withAccessScopes(Optional<List<String>> accessScopes)
		{
			this.accessScopes = accessScopes;
			return this;
		}

		public Builder withAccessGrantTime(Optional<Instant> accessGrantTime)
		{
			this.accessGrantTime = accessGrantTime;
			return this;
		}
		
		public Builder withAccessDeniedTime(Optional<Instant> accessDeniedTime)
		{
			this.accessDeniedTime = accessDeniedTime;
			return this;
		}

		public Builder withLastAccessTime(Optional<Instant> lastAccessTime)
		{
			this.lastAccessTime = lastAccessTime;
			return this;
		}

		public Builder withApplicationDomain(Optional<String> applicationDomain)
		{
			this.applicationDomain = applicationDomain;
			return this;
		}

		public Builder withLogo(Optional<byte[]> logo)
		{
			this.logo = logo;
			return this;
		}

		public Builder withTechnicalInformations(List<TechnicalInformationProperty> technicalInformations)
		{
			this.technicalInformations = technicalInformations;
			return this;
		}

		public IdPClientData build()
		{
			return new IdPClientData(this);
		}
	}

}
