/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.Objects;

import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;

class EvaluatorContext
{
	final User user;
	final Object arrayObj;
	final CachingMVELGroupProvider groupProvider;
	final SCIMEndpointDescription scimEndpointDescription;

	private EvaluatorContext(Builder builder)
	{
		this.user = builder.user;
		this.arrayObj = builder.arrayObj;
		this.groupProvider = builder.groupProvider;
		this.scimEndpointDescription = builder.scimEndpointDescription;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(arrayObj, groupProvider, scimEndpointDescription, user);
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
		EvaluatorContext other = (EvaluatorContext) obj;
		return Objects.equals(arrayObj, other.arrayObj) 
				&& Objects.equals(groupProvider, other.groupProvider)
				&& Objects.equals(scimEndpointDescription, other.scimEndpointDescription)
				&& Objects.equals(user, other.user);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private User user;
		private Object arrayObj;
		private CachingMVELGroupProvider groupProvider;
		private SCIMEndpointDescription scimEndpointDescription;

		private Builder()
		{
		}

		public Builder withUser(User user)
		{
			this.user = user;
			return this;
		}

		public Builder withArrayObj(Object arrayObj)
		{
			this.arrayObj = arrayObj;
			return this;
		}

		public Builder withGroupProvider(CachingMVELGroupProvider groupProvider)
		{
			this.groupProvider = groupProvider;
			return this;
		}

		public Builder withScimEndpointDescription(SCIMEndpointDescription scimEndpointDescription)
		{
			this.scimEndpointDescription = scimEndpointDescription;
			return this;
		}

		public EvaluatorContext build()
		{
			return new EvaluatorContext(this);
		}
	}

}