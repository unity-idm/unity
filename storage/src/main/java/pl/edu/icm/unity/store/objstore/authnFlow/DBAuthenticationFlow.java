/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.authnFlow;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAuthenticationFlow.Builder.class)
class DBAuthenticationFlow
{
	public final String name;
	public final Set<String> firstFactorAuthenticators;
	public final List<String> secondFactorAuthenticators;
	public final String policy;
	public final long revision;
	private DBAuthenticationFlow(Builder builder)
	{
		this.name = builder.name;
		this.firstFactorAuthenticators = Optional.ofNullable(builder.firstFactorAuthenticators)
				.map(Set::copyOf)
				.orElse(null);
		this.secondFactorAuthenticators = Optional.ofNullable(builder.secondFactorAuthenticators)
				.map(List::copyOf)
				.orElse(null);
		this.policy = builder.policy;
		this.revision = builder.revision;
	}
	
	
	@Override
	public int hashCode()
	{
		return Objects.hash(firstFactorAuthenticators, name, policy, revision, secondFactorAuthenticators);
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
		DBAuthenticationFlow other = (DBAuthenticationFlow) obj;
		return Objects.equals(firstFactorAuthenticators, other.firstFactorAuthenticators)
				&& Objects.equals(name, other.name) && Objects.equals(policy, other.policy)
				&& revision == other.revision
				&& Objects.equals(secondFactorAuthenticators, other.secondFactorAuthenticators);
	}



	public static Builder builder()
	{
		return new Builder();
	}
	public static final class Builder
	{
		private String name;
		private Set<String> firstFactorAuthenticators = Collections.emptySet();
		private List<String> secondFactorAuthenticators = Collections.emptyList();
		private String policy;
		private long revision;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withFirstFactorAuthenticators(Set<String> firstFactorAuthenticators)
		{
			this.firstFactorAuthenticators = firstFactorAuthenticators;
			return this;
		}

		public Builder withSecondFactorAuthenticators(List<String> secondFactorAuthenticators)
		{
			this.secondFactorAuthenticators = secondFactorAuthenticators;
			return this;
		}

		public Builder withPolicy(String policy)
		{
			this.policy = policy;
			return this;
		}

		public Builder withRevision(long revision)
		{
			this.revision = revision;
			return this;
		}

		public DBAuthenticationFlow build()
		{
			return new DBAuthenticationFlow(this);
		}
	}
}
