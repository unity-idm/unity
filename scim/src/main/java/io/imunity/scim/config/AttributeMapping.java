/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

//TODO
@JsonDeserialize(builder = AttributeMapping.Builder.class)
public class AttributeMapping
{

	private AttributeMapping(Builder builder)
	{
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Builder()
		{
		}

		public AttributeMapping build()
		{
			return new AttributeMapping(this);
		}
	}
}
