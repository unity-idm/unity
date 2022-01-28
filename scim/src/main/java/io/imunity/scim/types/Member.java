/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.types;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = Member.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Member
{
	public enum MemberType { Group, User};
	
	public final String value;
	@JsonProperty("$ref")
	public final URI ref;
	public final String display;
	public final MemberType type;

	private Member(Builder builder)
	{
		this.value = builder.value;
		this.ref = builder.ref;
		this.display = builder.display;
		this.type = builder.type;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String value;
		private URI ref;
		private String display;
		private MemberType type;
		
		private Builder()
		{
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public Builder withRef(URI ref)
		{
			this.ref = ref;
			return this;
		}

		public Builder withDisplay(String display)
		{
			this.display = display;
			return this;
		}
		
		public Builder withType(MemberType type)
		{
			this.type = type;
			return this;
		}

		public Member build()
		{
			return new Member(this);
		}
	}

}
