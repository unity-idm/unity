/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;

class EvaluatorContext
{
	final User user;
	final Object arrayObj;
	final CachingMVELGroupProvider groupProvider;
	final SCIMEndpointDescription scimEndpointDescription;

	private EvaluatorContext(EvaluatorContext.Builder builder)
	{
		this.user = builder.user;
		this.arrayObj = builder.arrayObj;
		this.groupProvider = builder.groupProvider;
		this.scimEndpointDescription = builder.scimEndpointDescription;
	}

	static EvaluatorContext.Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private User user;
		private Object arrayObj;
		private CachingMVELGroupProvider groupProvider;
		private SCIMEndpointDescription scimEndpointDescription;

		private Builder()
		{
		}

		public EvaluatorContext.Builder withUser(User user)
		{
			this.user = user;
			return this;
		}

		public EvaluatorContext.Builder withArrayObj(Object arrayObj)
		{
			this.arrayObj = arrayObj;
			return this;
		}

		public EvaluatorContext.Builder withGroupProvider(CachingMVELGroupProvider groupProvider)
		{
			this.groupProvider = groupProvider;
			return this;
		}

		public EvaluatorContext.Builder withScimEndpointDescription(SCIMEndpointDescription scimEndpointDescription)
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