/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.types;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = GroupResource.Builder.class)
public class GroupResource extends BasicScimResource
{
	public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
	
	public final String displayName;
	public final List<Member> members;

	private GroupResource(Builder builder)
	{
		super(builder);
		this.displayName = builder.displayName;
		this.members = builder.members;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private String displayName;
		private List<Member> members = Collections.emptyList();

		private Builder()
		{
			withSchemas(Schemas.of(SCHEMA));
		}

		public Builder withDisplayName(String displayName)
		{
			this.displayName = displayName;
			return this;
		}

		public Builder withMembers(List<Member> members)
		{
			this.members = members;
			return this;
		}

		public GroupResource build()
		{
			return new GroupResource(this);
		}
	}

}
