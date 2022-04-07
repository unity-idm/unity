/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.scim.common.BasicSCIMResource;
import io.imunity.scim.schema.DefaultSchemaProvider;

@JsonDeserialize(builder = SCIMUserResource.Builder.class)
class SCIMUserResource extends BasicSCIMResource
{
	static final String SCHEMA = DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID;

	@JsonAnyGetter
	public final Map<String, Object> attributes;

	private SCIMUserResource(Builder builder)
	{
		super(builder);
		this.attributes = new LinkedHashMap<>(builder.attributes);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private Map<String, Object> attributes = Collections.emptyMap();

		public Builder()
		{
			withSchemas(Set.of(SCHEMA));
		}

		public Builder withAttributes(Map<String, Object> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public SCIMUserResource build()
		{
			return new SCIMUserResource(this);
		}
	}

}
