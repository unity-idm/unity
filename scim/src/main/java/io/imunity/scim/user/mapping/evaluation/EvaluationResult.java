/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.Objects;
import java.util.Optional;

class EvaluationResult
{
	final String attributeName;
	final Optional<Object> value;

	private EvaluationResult(Builder builder)
	{
		this.attributeName = builder.attributeName;
		this.value = builder.value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributeName, value);
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
		EvaluationResult other = (EvaluationResult) obj;
		return Objects.equals(attributeName, other.attributeName) && Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String attributeName;
		private Optional<Object> value = Optional.empty();

		private Builder()
		{
		}

		public Builder withAttributeName(String attributeName)
		{
			this.attributeName = attributeName;
			return this;
		}

		public Builder withValue(Optional<Object> value)
		{
			this.value = value;
			return this;
		}

		public EvaluationResult build()
		{
			return new EvaluationResult(this);
		}
	}

}
