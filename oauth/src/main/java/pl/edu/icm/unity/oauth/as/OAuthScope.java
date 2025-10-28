/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;

public class OAuthScope
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthScope.class);
	
	public final String name;
	public final String description;
	public final List<String> attributes;
	public final boolean enabled;
	public final boolean wildcard;

	
	private OAuthScope(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.attributes = List.copyOf(builder.attributes);
		this.enabled = builder.enabled;
		this.wildcard = builder.wildcard;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, description, enabled, name, wildcard);
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
		OAuthScope other = (OAuthScope) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(description, other.description)
				&& enabled == other.enabled && wildcard == other.wildcard && Objects.equals(name, other.name);
	}
	
	public boolean match(String scope)
	{
		if (!wildcard)
			return name.equals(scope);

		try
		{
			Pattern pattern = Pattern.compile(name);
			return pattern.matcher(scope)
					.find();

		} catch (PatternSyntaxException e)
		{
			log.error("Incorrect pattern", e);
			return false;
		}
	}
	
	

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private List<String> attributes = Collections.emptyList();
		private boolean enabled;
		private boolean wildcard;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withAttributes(List<String> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public Builder withEnabled(boolean enabled)
		{
			this.enabled = enabled;
			return this;
		}

		public Builder withWildcard(boolean wildcard)
		{
			this.wildcard = wildcard;
			return this;
		}
		
		public OAuthScope build()
		{
			return new OAuthScope(this);
		}
	}

}