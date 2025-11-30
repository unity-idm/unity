package pl.edu.icm.unity.oauth.as;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record ActiveOAuthScopeDefinition(
		String name,
		String description,
		List<String> attributes,
		boolean pattern)
{

	public ActiveOAuthScopeDefinition
	{
		attributes = Optional.ofNullable(attributes)
				.map(List::copyOf)
				.orElse(null);
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
		private boolean pattern;

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

		public Builder withPattern(boolean pattern)
		{
			this.pattern = pattern;
			return this;
		}

		public ActiveOAuthScopeDefinition build()
		{
			return new ActiveOAuthScopeDefinition(name, description, attributes, pattern);
		}
	}
}